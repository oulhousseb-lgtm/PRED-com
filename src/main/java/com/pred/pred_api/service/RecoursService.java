package com.pred.pred_api.service;

import com.pred.pred_api.dto.RecoursRequest;
import com.pred.pred_api.dto.RecoursResponse;
import com.pred.pred_api.dto.ChangerStatutRequest;
import com.pred.pred_api.exception.ResourceNotFoundException;
import com.pred.pred_api.model.*;
import com.pred.pred_api.model.enums.*;
import com.pred.pred_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;   // ← IMPORT IMPORTANT
import java.util.List;
import java.util.Map;       // ← IMPORT IMPORTANT
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecoursService {

    private final RecoursRepository recoursRepository;
    private final UserRepository userRepository;
    private final TypeRecoursService typeRecoursService;
    private final AppelantRepository appelantRepository;
    private final AccuseRepository accuseRepository;
    private final TemoinRepository temoinRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final HistoriqueStatutRepository historiqueStatutRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    private final String uploadDir = "./uploads/";

    // ============================================================
    // Création de recours
    // ============================================================

    @Transactional
    public RecoursResponse createRecours(Long utilisateurId, RecoursRequest request) {
        User utilisateur = userRepository.findById(utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        TypeRecours typeRecours = typeRecoursService.findEntityById(request.getTypeRecoursId());

        // Génération du numéro de recours
        String numeroRecours = generateNumeroRecours();

        // Création du recours principal
        Recours recours = Recours.builder()
                .numeroRecours(numeroRecours)
                .utilisateur(utilisateur)
                .typeRecours(typeRecours)
                .numeroDecisionAttaque(request.getNumeroDecisionAttaque())
                .dateDecisionAttaque(request.getDateDecisionAttaque())
                .juridictionSourceFr(request.getJuridictionSourceFr())
                .juridictionSourceAr(request.getJuridictionSourceAr())
                .moyensRecoursFr(request.getMoyensRecoursFr())
                .moyensRecoursAr(request.getMoyensRecoursAr())
                .chambre(request.getChambre())
                .statut(StatutRecours.DEPOSE)
                .dateDepot(LocalDateTime.now())
                .build();

        Recours savedRecours = recoursRepository.save(recours);

        // Ajout des appelants
        if (request.getAppelants() != null && !request.getAppelants().isEmpty()) {
            for (RecoursRequest.AppelantDTO dto : request.getAppelants()) {
                Appelant appelant = createAppelantFromDTO(savedRecours, dto);
                appelantRepository.save(appelant);
            }
        } else {
            // Ajouter automatiquement le déposant comme appelant principal
            Appelant deposantAsAppelant = createAppelantFromUser(savedRecours, utilisateur);
            appelantRepository.save(deposantAsAppelant);
        }

        // Ajout des accusés
        if (request.getAccuses() != null && !request.getAccuses().isEmpty()) {
            for (RecoursRequest.AccuseDTO dto : request.getAccuses()) {
                Accuse accuse = createAccuseFromDTO(savedRecours, dto);
                accuseRepository.save(accuse);
            }
        }

        // Ajout des témoins
        if (request.getTemoins() != null && !request.getTemoins().isEmpty()) {
            for (RecoursRequest.TemoinDTO dto : request.getTemoins()) {
                Temoin temoin = createTemoinFromDTO(savedRecours, dto);
                temoinRepository.save(temoin);
            }
        }

        // Enregistrement dans l'historique
        saveHistoriqueStatut(savedRecours, null, StatutRecours.DEPOSE, utilisateur,
                "Dépôt initial du recours", "إيداع أولي للطعن");

        // Notification au greffe
        notificationService.notifierGreffeNouveauRecours(savedRecours);

        // Audit log
        auditLogService.logAction(utilisateur, "CREATE_RECOURS",
                "Création du recours n° " + numeroRecours);

        return toResponse(savedRecours);
    }

    private String generateNumeroRecours() {
        String annee = String.valueOf(LocalDate.now().getYear());
        long count = recoursRepository.countByAnnee(LocalDate.now().getYear());
        return String.format("PRED-%s-%06d", annee, count + 1);
    }

    // ============================================================
    // Gestion des parties
    // ============================================================

    private Appelant createAppelantFromUser(Recours recours, User user) {
        return Appelant.builder()
                .recours(recours)
                .utilisateur(user)
                .cin(user.getCin())
                .nomFr(user.getNomFr())
                .prenomFr(user.getPrenomFr())
                .nomAr(user.getNomAr())
                .prenomAr(user.getPrenomAr())
                .genre(user.getGenre())
                .dateNaissance(user.getDateNaissance())
                .lieuNaissanceFr(user.getLieuNaissanceFr())
                .lieuNaissanceAr(user.getLieuNaissanceAr())
                .situationFamiliale(user.getSituationFamiliale())
                .professionFr(user.getProfessionFr())
                .professionAr(user.getProfessionAr())
                .adresseFr(user.getAdresseFr())
                .adresseAr(user.getAdresseAr())
                .estMajeur(user.getEstMajeur())
                .qualiteFr("Appelant principal")
                .qualiteAr("مستأنف رئيسي")
                .build();
    }

    private Appelant createAppelantFromDTO(Recours recours, RecoursRequest.AppelantDTO dto) {
        User utilisateur = null;
        if (dto.getUtilisateurId() != null) {
            utilisateur = userRepository.findById(dto.getUtilisateurId()).orElse(null);
        }

        return Appelant.builder()
                .recours(recours)
                .utilisateur(utilisateur)
                .cin(dto.getCin())
                .nomFr(dto.getNomFr())
                .prenomFr(dto.getPrenomFr())
                .nomAr(dto.getNomAr())
                .prenomAr(dto.getPrenomAr())
                .genre(dto.getGenre() != null ? Genre.valueOf(dto.getGenre()) : null)
                .dateNaissance(dto.getDateNaissance())
                .lieuNaissanceFr(dto.getLieuNaissanceFr())
                .lieuNaissanceAr(dto.getLieuNaissanceAr())
                .situationFamiliale(dto.getSituationFamiliale() != null ?
                        SituationFamiliale.valueOf(dto.getSituationFamiliale()) : null)
                .professionFr(dto.getProfessionFr())
                .professionAr(dto.getProfessionAr())
                .adresseFr(dto.getAdresseFr())
                .adresseAr(dto.getAdresseAr())
                .estMajeur(dto.getEstMajeur() != null ? dto.getEstMajeur() : true)
                .qualiteFr(dto.getQualiteFr())
                .qualiteAr(dto.getQualiteAr())
                .build();
    }

    private Accuse createAccuseFromDTO(Recours recours, RecoursRequest.AccuseDTO dto) {
        return Accuse.builder()
                .recours(recours)
                .cin(dto.getCin())
                .nomFr(dto.getNomFr())
                .prenomFr(dto.getPrenomFr())
                .nomAr(dto.getNomAr())
                .prenomAr(dto.getPrenomAr())
                .genre(dto.getGenre() != null ? Genre.valueOf(dto.getGenre()) : null)
                .dateNaissance(dto.getDateNaissance())
                .lieuNaissanceFr(dto.getLieuNaissanceFr())
                .lieuNaissanceAr(dto.getLieuNaissanceAr())
                .situationFamiliale(dto.getSituationFamiliale() != null ?
                        SituationFamiliale.valueOf(dto.getSituationFamiliale()) : null)
                .professionFr(dto.getProfessionFr())
                .professionAr(dto.getProfessionAr())
                .adresseFr(dto.getAdresseFr())
                .adresseAr(dto.getAdresseAr())
                .estMajeur(dto.getEstMajeur() != null ? dto.getEstMajeur() : true)
                .situationPenale(dto.getSituationPenale() != null ?
                        SituationPenale.valueOf(dto.getSituationPenale()) : SituationPenale.LIBRE)
                .lieuDetention(dto.getLieuDetention())
                .qualificationFr(dto.getQualificationFr())
                .qualificationAr(dto.getQualificationAr())
                .build();
    }

    private Temoin createTemoinFromDTO(Recours recours, RecoursRequest.TemoinDTO dto) {
        return Temoin.builder()
                .recours(recours)
                .cin(dto.getCin())
                .nomFr(dto.getNomFr())
                .prenomFr(dto.getPrenomFr())
                .nomAr(dto.getNomAr())
                .prenomAr(dto.getPrenomAr())
                .professionFr(dto.getProfessionFr())
                .professionAr(dto.getProfessionAr())
                .adresseFr(dto.getAdresseFr())
                .adresseAr(dto.getAdresseAr())
                .telephone(dto.getTelephone())
                .temoignageFr(dto.getTemoignageFr())
                .temoignageAr(dto.getTemoignageAr())
                .build();
    }

    // ============================================================
    // Recherche
    // ============================================================

    public RecoursResponse findById(Long id) {
        Recours recours = recoursRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recours non trouvé avec l'ID : " + id));
        return toDetailedResponse(recours);
    }

    public Recours findEntityById(Long id) {
        return recoursRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recours non trouvé avec l'ID : " + id));
    }
    @Transactional(readOnly = true)
    public List<RecoursResponse> findByUtilisateur(Long utilisateurId) {
        User utilisateur = userRepository.findById(utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        return recoursRepository.findByUtilisateur(utilisateur).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<RecoursResponse> findByStatut(StatutRecours statut) {
        return recoursRepository.findByStatut(statut).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecoursResponse> findAll() {
        return recoursRepository.findAllWithDetails().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // Gestion des statuts
    // ============================================================

    @Transactional
    public RecoursResponse changerStatut(Long recoursId, ChangerStatutRequest request, User modifiePar) {
        Recours recours = findEntityById(recoursId);
        StatutRecours ancienStatut = recours.getStatut();
        StatutRecours nouveauStatut = StatutRecours.valueOf(request.getNouveauStatut());

        recours.setStatut(nouveauStatut);

        if (nouveauStatut == StatutRecours.JUGE || nouveauStatut == StatutRecours.REJETE) {
            recours.setDateJugement(LocalDate.now());
        }

        Recours savedRecours = recoursRepository.save(recours);

        // Enregistrement dans l'historique
        saveHistoriqueStatut(savedRecours, ancienStatut, nouveauStatut, modifiePar,
                request.getCommentaireFr(), request.getCommentaireAr());

        // Notification au justiciable
        notificationService.notifierChangementStatut(savedRecours, ancienStatut, nouveauStatut);

        // Audit log
        auditLogService.logAction(modifiePar, "CHANGE_STATUT",
                "Changement de statut du recours n° " + recours.getNumeroRecours() +
                        " de " + ancienStatut + " à " + nouveauStatut);

        return toResponse(savedRecours);
    }

    private void saveHistoriqueStatut(Recours recours, StatutRecours ancienStatut,
                                      StatutRecours nouveauStatut, User modifiePar,
                                      String commentaireFr, String commentaireAr) {
        HistoriqueStatut historique = HistoriqueStatut.builder()
                .recours(recours)
                .ancienStatut(ancienStatut != null ? ancienStatut.name() : "CREATION")
                .nouveauStatut(nouveauStatut.name())
                .modifiePar(modifiePar)
                .commentaireFr(commentaireFr)
                .commentaireAr(commentaireAr)
                .dateModification(LocalDateTime.now())
                .build();

        historiqueStatutRepository.save(historique);
    }

    // ============================================================
    // Gestion des décisions finales
    // ============================================================

    @Transactional
    public RecoursResponse ajouterDecision(Long recoursId, String decisionFr, String decisionAr,
                                           MultipartFile fichier, User modifiePar) throws IOException {
        Recours recours = findEntityById(recoursId);

        recours.setDecisionFinaleFr(decisionFr);
        recours.setDecisionFinaleAr(decisionAr);

        if (fichier != null && !fichier.isEmpty()) {
            String cheminFichier = uploadFile(fichier, "decisions");
            recours.setFichierDecision(cheminFichier);
        }

        StatutRecours ancienStatut = recours.getStatut();
        recours.setStatut(StatutRecours.JUGE);
        recours.setDateJugement(LocalDate.now());

        Recours savedRecours = recoursRepository.save(recours);

        saveHistoriqueStatut(savedRecours, ancienStatut, StatutRecours.JUGE, modifiePar,
                "Décision rendue", "تم البت في القضية");

        notificationService.notifierDecisionRendue(savedRecours);

        auditLogService.logAction(modifiePar, "AJOUT_DECISION",
                "Ajout de la décision pour le recours n° " + recours.getNumeroRecours());

        return toResponse(savedRecours);
    }

// ============================================================
// REMPLACER les 3 méthodes ci-dessus par ceci :
// ============================================================

// ============================================================
// Gestion des pièces jointes (version avec chemin hiérarchique)
// ============================================================

    @Transactional
    public PieceJointe uploadPiece(Long recoursId, MultipartFile file, TypeDocument typeDocument,
                                   String descriptionFr, String descriptionAr, String cheminStockage) throws IOException {
        Recours recours = findEntityById(recoursId);

        // Si un chemin de stockage est fourni (ex: "2026/2609/72/"), l'utiliser
        // Sinon, utiliser "pieces/" comme avant
        String dossierStockage = (cheminStockage != null && !cheminStockage.isEmpty())
                ? cheminStockage
                : "pieces";

        String cheminFichier = uploadFile(file, dossierStockage);

        PieceJointe pieceJointe = PieceJointe.builder()
                .recours(recours)
                .nomFichier(file.getOriginalFilename())
                .cheminFichier(cheminFichier)
                .cheminStockage(dossierStockage)  // Stocker le chemin hiérarchique
                .typeDocument(typeDocument)
                .descriptionFr(descriptionFr)
                .descriptionAr(descriptionAr)
                .tailleOctets(file.getSize())
                .hashSha256(calculateHash(file))
                .build();

        return pieceJointeRepository.save(pieceJointe);
    }

    // Surcharge pour rétrocompatibilité (sans cheminStockage)
    @Transactional
    public PieceJointe uploadPiece(Long recoursId, MultipartFile file, TypeDocument typeDocument,
                                   String descriptionFr, String descriptionAr) throws IOException {
        return uploadPiece(recoursId, file, typeDocument, descriptionFr, descriptionAr, "pieces");
    }

    private String uploadFile(MultipartFile file, String sousDossier) throws IOException {
        // Construire le chemin complet : uploadDir/sousDossier/
        Path uploadPath = Paths.get(uploadDir, sousDossier);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        return filePath.toString();
    }

    private String calculateHash(MultipartFile file) {
        // Implémentation simplifiée - à compléter avec SHA-256
        return UUID.randomUUID().toString().replace("-", "");
    }
    // ============================================================
    // Statistiques
    // ============================================================

    public long countByStatut(StatutRecours statut) {
        return recoursRepository.countByStatut(statut);
    }

    public java.util.Map<StatutRecours, Long> getStatistiquesParStatut() {
        return java.util.Arrays.stream(StatutRecours.values())
                .collect(Collectors.toMap(
                        statut -> statut,
                        this::countByStatut
                ));
    }

    // ============================================================
    // Conversion DTO
    // ============================================================

    // ============================================================
// CORRECTION FINALE : toResponse() avec gestion des listes null
// et accès sécurisé aux objets imbriqués
// ============================================================
    private RecoursResponse toResponse(Recours recours) {
        return RecoursResponse.builder()
                .id(recours.getId())
                .numeroRecours(recours.getNumeroRecours())

                // Accès sécurisé au typeRecours
                .typeRecoursId(recours.getTypeRecours() != null ? recours.getTypeRecours().getId() : null)
                .typeRecoursCode(recours.getTypeRecours() != null ? recours.getTypeRecours().getCode() : null)
                .typeRecoursLibelleFr(recours.getTypeRecours() != null ? recours.getTypeRecours().getLibelleFr() : null)
                .typeRecoursLibelleAr(recours.getTypeRecours() != null ? recours.getTypeRecours().getLibelleAr() : null)
                .typeRecoursCategorie(recours.getTypeRecours() != null ? recours.getTypeRecours().getCategorie() : null)

                // Accès sécurisé à l'utilisateur
                .deposantId(recours.getUtilisateur() != null ? recours.getUtilisateur().getId() : null)
                .deposantNomFr(recours.getUtilisateur() != null ? recours.getUtilisateur().getNomFr() : null)
                .deposantPrenomFr(recours.getUtilisateur() != null ? recours.getUtilisateur().getPrenomFr() : null)
                .deposantNomAr(recours.getUtilisateur() != null ? recours.getUtilisateur().getNomAr() : null)
                .deposantPrenomAr(recours.getUtilisateur() != null ? recours.getUtilisateur().getPrenomAr() : null)
                .deposantEmail(recours.getUtilisateur() != null ? recours.getUtilisateur().getEmail() : null)

                // Champs simples (pas de risque NPE)
                .numeroDecisionAttaque(recours.getNumeroDecisionAttaque())
                .dateDecisionAttaque(recours.getDateDecisionAttaque())
                .juridictionSourceFr(recours.getJuridictionSourceFr())
                .juridictionSourceAr(recours.getJuridictionSourceAr())
                .moyensRecoursFr(recours.getMoyensRecoursFr())
                .moyensRecoursAr(recours.getMoyensRecoursAr())
                .fichierMoyens(recours.getFichierMoyens())
                .statut(recours.getStatut())
                .dateDepot(recours.getDateDepot())
                .dateAudience(recours.getDateAudience())
                .dateJugement(recours.getDateJugement())
                .decisionFinaleFr(recours.getDecisionFinaleFr())
                .decisionFinaleAr(recours.getDecisionFinaleAr())
                .fichierDecision(recours.getFichierDecision())
                .chambre(recours.getChambre())

                // Listes avec vérification null
                .nombreAppelants(recours.getAppelants() != null ? recours.getAppelants().size() : 0)
                .nombreAccuses(recours.getAccuses() != null ? recours.getAccuses().size() : 0)
                .nombreTemoins(recours.getTemoins() != null ? recours.getTemoins().size() : 0)
                .nombrePiecesJointes(recours.getPiecesJointes() != null ? recours.getPiecesJointes().size() : 0)
                .build();
    }
    private RecoursResponse toDetailedResponse(Recours recours) {
        RecoursResponse response = toResponse(recours);

        // Ajout des listes détaillées
        response.setAppelants(recours.getAppelants().stream()
                .map(this::toAppelantResponse)
                .collect(Collectors.toList()));

        response.setAccuses(recours.getAccuses().stream()
                .map(this::toAccuseResponse)
                .collect(Collectors.toList()));

        response.setTemoins(recours.getTemoins().stream()
                .map(this::toTemoinResponse)
                .collect(Collectors.toList()));

        response.setPiecesJointes(recours.getPiecesJointes().stream()
                .map(this::toPieceJointeResponse)
                .collect(Collectors.toList()));

        response.setHistorique(historiqueStatutRepository.findByRecoursOrderByDateModificationDesc(recours).stream()
                .map(this::toHistoriqueResponse)
                .collect(Collectors.toList()));

        return response;
    }

    private RecoursResponse.AppelantResponseDTO toAppelantResponse(Appelant appelant) {
        return RecoursResponse.AppelantResponseDTO.builder()
                .id(appelant.getId())
                .cin(appelant.getCin())
                .nomFr(appelant.getNomFr())
                .prenomFr(appelant.getPrenomFr())
                .nomAr(appelant.getNomAr())
                .prenomAr(appelant.getPrenomAr())
                .qualiteFr(appelant.getQualiteFr())
                .qualiteAr(appelant.getQualiteAr())
                .estMajeur(appelant.getEstMajeur())
                .build();
    }

    private RecoursResponse.AccuseResponseDTO toAccuseResponse(Accuse accuse) {
        return RecoursResponse.AccuseResponseDTO.builder()
                .id(accuse.getId())
                .cin(accuse.getCin())
                .nomFr(accuse.getNomFr())
                .prenomFr(accuse.getPrenomFr())
                .nomAr(accuse.getNomAr())
                .prenomAr(accuse.getPrenomAr())
                .situationPenale(accuse.getSituationPenale() != null ? accuse.getSituationPenale().name() : null)
                .lieuDetention(accuse.getLieuDetention())
                .qualificationFr(accuse.getQualificationFr())
                .qualificationAr(accuse.getQualificationAr())
                .estMajeur(accuse.getEstMajeur())
                .build();
    }

    private RecoursResponse.TemoinResponseDTO toTemoinResponse(Temoin temoin) {
        return RecoursResponse.TemoinResponseDTO.builder()
                .id(temoin.getId())
                .cin(temoin.getCin())
                .nomFr(temoin.getNomFr())
                .prenomFr(temoin.getPrenomFr())
                .nomAr(temoin.getNomAr())
                .prenomAr(temoin.getPrenomAr())
                .professionFr(temoin.getProfessionFr())
                .professionAr(temoin.getProfessionAr())
                .telephone(temoin.getTelephone())
                .temoignageFr(temoin.getTemoignageFr())
                .temoignageAr(temoin.getTemoignageAr())
                .build();
    }

    private RecoursResponse.PieceJointeResponseDTO toPieceJointeResponse(PieceJointe piece) {
        return RecoursResponse.PieceJointeResponseDTO.builder()
                .id(piece.getId())
                .nomFichier(piece.getNomFichier())
                .typeDocument(piece.getTypeDocument() != null ? piece.getTypeDocument().name() : null)
                .descriptionFr(piece.getDescriptionFr())
                .descriptionAr(piece.getDescriptionAr())
                .dateUpload(piece.getDateUpload())
                .tailleOctets(piece.getTailleOctets())
                .build();
    }

    private RecoursResponse.HistoriqueStatutResponseDTO toHistoriqueResponse(HistoriqueStatut historique) {
        return RecoursResponse.HistoriqueStatutResponseDTO.builder()
                .id(historique.getId())
                .ancienStatut(historique.getAncienStatut())
                .nouveauStatut(historique.getNouveauStatut())
                .modifieParNom(historique.getModifiePar().getFullNameFr())
                .dateModification(historique.getDateModification())
                .commentaireFr(historique.getCommentaireFr())
                .commentaireAr(historique.getCommentaireAr())
                .build();
    }
    // Ajouter ces méthodes dans RecoursService.java

    public RecoursResponse findByNumeroRecours(String numero) {
        Recours recours = recoursRepository.findByNumeroRecours(numero)
                .orElseThrow(() -> new ResourceNotFoundException("Recours non trouvé avec le numéro : " + numero));
        return toDetailedResponse(recours);
    }

    public RecoursResponse findDetailedById(Long id) {
        Recours recours = findEntityById(id);
        return toDetailedResponse(recours);
    }

    public long countAll() {
        return recoursRepository.count();
    }

    public long countByUtilisateur(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        return recoursRepository.countByUtilisateur(user);
    }

    public Map<String, Long> getStatistiquesParStatutForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        List<Object[]> results = recoursRepository.countByStatutGroupByStatutForUser(user);
        Map<String, Long> stats = new HashMap<>();
        for (Object[] result : results) {
            stats.put(result[0].toString(), (Long) result[1]);
        }
        return stats;
    }

    public List<RecoursResponse> findRecentRecours(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("dateDepot").descending());
        return recoursRepository.findRecentRecours(pageable).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void ajouterNotesInternes(Long id, String notes) {
        Recours recours = findEntityById(id);
        recours.setNotesInternes(notes);
        recoursRepository.save(recours);
    }

    @Transactional
    public void envoyerNotificationManuelle(Long recoursId, String titreFr, String messageFr,
                                            String titreAr, String messageAr) {
        Recours recours = findEntityById(recoursId);
        notificationService.notifierUtilisateur(recours.getUtilisateur(), recours,
                titreFr, messageFr, titreAr, messageAr, TypeNotification.SYSTEME);
    }

    public List<RecoursResponse> rechercheAvancee(String numero, String cin, String nom,
                                                  String statut, String dateDebut, String dateFin) {
        // Implémentation simplifiée - à enrichir
        if (numero != null && !numero.isEmpty()) {
            return recoursRepository.findByNumeroRecours(numero)
                    .map(recours -> List.of(toResponse(recours)))
                    .orElse(List.of());
        }
        return findAll();
    }

    public Double getDelaiMoyenTraitement() {
        return historiqueStatutRepository.getDelaiMoyenTraitement();
    }


}