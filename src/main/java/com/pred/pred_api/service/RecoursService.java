package com.pred.pred_api.service;
import com.pred.pred_api.repository.DossierRepository;
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
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
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
    // Injection du repository
    private final VictimeRepository victimeRepository;
    private final DossierRepository dossierRepository;
    private final String uploadDir = "./uploads/";

    // ============================================================
    // Création de recours
    // ============================================================

    @Transactional
    public RecoursResponse createRecours(Long utilisateurId, RecoursRequest request) {
        User utilisateur = userRepository.findById(utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        TypeRecours typeRecours = typeRecoursService.findEntityById(request.getTypeRecoursId());

        String numeroRecours = generateNumeroRecours();

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

        if (request.getAppelants() != null && !request.getAppelants().isEmpty()) {
            for (RecoursRequest.AppelantDTO dto : request.getAppelants()) {
                Appelant appelant = createAppelantFromDTO(savedRecours, dto);
                appelantRepository.save(appelant);
            }
        } else {
            Appelant deposantAsAppelant = createAppelantFromUser(savedRecours, utilisateur);
            appelantRepository.save(deposantAsAppelant);
        }

        if (request.getAccuses() != null && !request.getAccuses().isEmpty()) {
            for (RecoursRequest.AccuseDTO dto : request.getAccuses()) {
                Accuse accuse = createAccuseFromDTO(savedRecours, dto);
                accuseRepository.save(accuse);
            }
        }

        if (request.getTemoins() != null && !request.getTemoins().isEmpty()) {
            for (RecoursRequest.TemoinDTO dto : request.getTemoins()) {
                Temoin temoin = createTemoinFromDTO(savedRecours, dto);
                temoinRepository.save(temoin);
            }
        }

        if (request.getVictimes() != null && !request.getVictimes().isEmpty()) {
            for (RecoursRequest.VictimeDTO dto : request.getVictimes()) {
                Victime victime = createVictimeFromDTO(savedRecours, dto);
                victimeRepository.save(victime);
            }
        }

        saveHistoriqueStatut(savedRecours, null, StatutRecours.DEPOSE, utilisateur,
                "Dépôt initial du recours", "إيداع أولي للطعن");

        notificationService.notifierGreffeNouveauRecours(savedRecours);

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


    // Méthode simplifiée (sans NaturePrejudice Enum)
    private Victime createVictimeFromDTO(Recours recours, RecoursRequest.VictimeDTO dto) {
        return Victime.builder()
                .recours(recours)
                .cin(dto.getCin())
                .nomFr(dto.getNomFr())
                .prenomFr(dto.getPrenomFr())
                .nomAr(dto.getNomAr())
                .prenomAr(dto.getPrenomAr())
                .genre(dto.getGenre() != null ? Genre.valueOf(dto.getGenre()) : Genre.HOMME)
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
                .naturePrejudice(dto.getNaturePrejudice()) // String direct
                .descriptionPrejudiceFr(dto.getDescriptionPrejudiceFr())
                .descriptionPrejudiceAr(dto.getDescriptionPrejudiceAr())
                .tuteurNomFr(dto.getTuteurNomFr())
                .tuteurPrenomFr(dto.getTuteurPrenomFr())
                .tuteurNomAr(dto.getTuteurNomAr())
                .tuteurPrenomAr(dto.getTuteurPrenomAr())
                .tuteurCin(dto.getTuteurCin())
                .build();
    }

    // ============================================================
    // Recherche
    // ============================================================

    // ✅ AJOUT : @Transactional(readOnly = true)
    @Transactional(readOnly = true)
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

        // ✅ حفظ تاريخ الجلسة
        if (request.getDateAudience() != null && !request.getDateAudience().isEmpty()) {
            try {
                LocalDate audienceDate = LocalDate.parse(request.getDateAudience());
                recours.setDateAudience(audienceDate);
            } catch (Exception e) {
                System.err.println("Erreur parsing dateAudience: " + request.getDateAudience());
            }
        }

        // ✅ حفظ أعضاء الهيئة القضائية
        if (request.getPresidentNom() != null && !request.getPresidentNom().isEmpty()) {
            recours.setPresidentNom(request.getPresidentNom());
        }
        if (request.getMembre1Nom() != null && !request.getMembre1Nom().isEmpty()) {
            recours.setMembre1Nom(request.getMembre1Nom());
        }
        if (request.getMembre2Nom() != null && !request.getMembre2Nom().isEmpty()) {
            recours.setMembre2Nom(request.getMembre2Nom());
        }
        if (request.getRepresentantMinistere() != null && !request.getRepresentantMinistere().isEmpty()) {
            recours.setRepresentantMinistere(request.getRepresentantMinistere());
        }
        if (request.getGreffierAudience() != null && !request.getGreffierAudience().isEmpty()) {
            recours.setGreffierAudience(request.getGreffierAudience());
        }

        // ✅✅✅ حفظ القرار النهائي في جدول recours ✅✅✅
        if (request.getDecisionFinaleFr() != null && !request.getDecisionFinaleFr().isEmpty()) {
            recours.setDecisionFinaleFr(request.getDecisionFinaleFr());
        }
        if (request.getDecisionFinaleAr() != null && !request.getDecisionFinaleAr().isEmpty()) {
            recours.setDecisionFinaleAr(request.getDecisionFinaleAr());
        }

        if (nouveauStatut == StatutRecours.JUGE || nouveauStatut == StatutRecours.REJETE) {
            recours.setDateJugement(LocalDate.now());
        }

        Recours savedRecours = recoursRepository.save(recours);

        saveHistoriqueStatut(savedRecours, ancienStatut, nouveauStatut, modifiePar,
                request.getCommentaireFr(), request.getCommentaireAr());

        notificationService.notifierChangementStatut(savedRecours, ancienStatut, nouveauStatut);

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
    // Gestion des décisions finales - CORRIGÉ
    // ============================================================

    @Transactional
    public RecoursResponse ajouterDecision(Long recoursId, String decisionFr, String decisionAr,
                                           MultipartFile fichier, User modifiePar) throws IOException {
        Recours recours = findEntityById(recoursId);

        recours.setDecisionFinaleFr(decisionFr);
        recours.setDecisionFinaleAr(decisionAr);

        if (fichier != null && !fichier.isEmpty()) {
            // Utiliser le chemin hiérarchique au lieu de "decisions"
            String cheminDecision = buildCheminFromRecours(recours);
            String cheminFichier = uploadFile(fichier, cheminDecision);
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
    // Gestion des pièces jointes (version avec chemin hiérarchique)
    // ============================================================

    @Transactional
    public PieceJointe uploadPiece(Long recoursId, MultipartFile file, TypeDocument typeDocument,
                                   String descriptionFr, String descriptionAr, String cheminStockage) throws IOException {
        Recours recours = findEntityById(recoursId);

        // Construire le chemin de stockage si non fourni
        String dossierStockage;
        if (cheminStockage != null && !cheminStockage.isEmpty() && !cheminStockage.equals("pieces")) {
            dossierStockage = cheminStockage;
        } else {
            dossierStockage = buildCheminFromRecours(recours);
        }

        String cheminFichier = uploadFile(file, dossierStockage);

        PieceJointe pieceJointe = PieceJointe.builder()
                .recours(recours)
                .nomFichier(file.getOriginalFilename())
                .cheminFichier(cheminFichier)
                .cheminStockage(dossierStockage)
                .typeDocument(typeDocument)
                .descriptionFr(descriptionFr)
                .descriptionAr(descriptionAr)
                .tailleOctets(file.getSize())
                .hashSha256(calculateHash(file))
                .build();

        return pieceJointeRepository.save(pieceJointe);
    }

    // Surcharge pour rétrocompatibilité
    @Transactional
    public PieceJointe uploadPiece(Long recoursId, MultipartFile file, TypeDocument typeDocument,
                                   String descriptionFr, String descriptionAr) throws IOException {
        return uploadPiece(recoursId, file, typeDocument, descriptionFr, descriptionAr, null);
    }

    // ============================================================
    // Méthodes utilitaires d'upload
    // ============================================================

    /**
     * Construit le chemin de stockage à partir du numéro de décision attaquée
     * Format : "SEQ/CODE/ANNEE" -> "ANNEE/CODE/SEQ/"
     */
    private String buildCheminFromRecours(Recours recours) {
        if (recours.getNumeroDecisionAttaque() != null && recours.getNumeroDecisionAttaque().contains("/")) {
            String[] parts = recours.getNumeroDecisionAttaque().split("/");
            if (parts.length == 3) {
                return parts[2] + "/" + parts[1] + "/" + parts[0] + "/";
            }
        }

        // Fallback
        String annee = String.valueOf(LocalDate.now().getYear());
        String code = recours.getTypeRecours() != null ? recours.getTypeRecours().getCode() : "AUTRE";
        String seq = String.valueOf(recours.getId());
        return annee + "/" + code + "/" + seq + "/";
    }

    private String uploadFile(MultipartFile file, String sousDossier) throws IOException {
        String cleanPath = sousDossier.replaceAll("^/+|/+$", "");

        Path uploadPath = Paths.get(uploadDir, cleanPath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            System.out.println("[UPLOAD] Dossier créé: " + uploadPath.toAbsolutePath());
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + extension;

        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("[UPLOAD] Fichier sauvegardé: " + filePath.toAbsolutePath());

        return filePath.toString();
    }

    private String calculateHash(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    // ============================================================
    // Statistiques
    // ============================================================

    public long countByStatut(StatutRecours statut) {
        return recoursRepository.countByStatut(statut);
    }

    public Map<StatutRecours, Long> getStatistiquesParStatut() {
        return Arrays.stream(StatutRecours.values())
                .collect(Collectors.toMap(statut -> statut, this::countByStatut));
    }

    // ============================================================
    // Conversion DTO
    // ============================================================

    private RecoursResponse toResponse(Recours recours) {
        return RecoursResponse.builder()
                .id(recours.getId())
                .numeroRecours(recours.getNumeroRecours())
                // ✅ AJOUT : dossier
                .dossierId(recours.getDossier() != null ? recours.getDossier().getId() : null)
                .dossierNumero(recours.getDossier() != null ? recours.getDossier().getNumeroDossier() : null)
                .dossierTitreFr(recours.getDossier() != null ? recours.getDossier().getTitreFr() : null)
                .dossierTitreAr(recours.getDossier() != null ? recours.getDossier().getTitreAr() : null)
                // Type recours
                .typeRecoursId(recours.getTypeRecours() != null ? recours.getTypeRecours().getId() : null)
                .typeRecoursCode(recours.getTypeRecours() != null ? recours.getTypeRecours().getCode() : null)
                .typeRecoursLibelleFr(recours.getTypeRecours() != null ? recours.getTypeRecours().getLibelleFr() : null)
                .typeRecoursLibelleAr(recours.getTypeRecours() != null ? recours.getTypeRecours().getLibelleAr() : null)
                .typeRecoursCategorie(recours.getTypeRecours() != null ? recours.getTypeRecours().getCategorie() : null)
                // Déposant
                .deposantId(recours.getUtilisateur() != null ? recours.getUtilisateur().getId() : null)
                .deposantNomFr(recours.getUtilisateur() != null ? recours.getUtilisateur().getNomFr() : null)
                .deposantPrenomFr(recours.getUtilisateur() != null ? recours.getUtilisateur().getPrenomFr() : null)
                .deposantNomAr(recours.getUtilisateur() != null ? recours.getUtilisateur().getNomAr() : null)
                .deposantPrenomAr(recours.getUtilisateur() != null ? recours.getUtilisateur().getPrenomAr() : null)
                .deposantEmail(recours.getUtilisateur() != null ? recours.getUtilisateur().getEmail() : null)
                // Décision attaquée
                .numeroDecisionAttaque(recours.getNumeroDecisionAttaque())
                .dateDecisionAttaque(recours.getDateDecisionAttaque())
                .juridictionSourceFr(recours.getJuridictionSourceFr())
                .juridictionSourceAr(recours.getJuridictionSourceAr())
                .moyensRecoursFr(recours.getMoyensRecoursFr())
                .moyensRecoursAr(recours.getMoyensRecoursAr())
                .fichierMoyens(recours.getFichierMoyens())
                // Statut et dates
                .statut(recours.getStatut())
                .dateDepot(recours.getDateDepot())
                .dateAudience(recours.getDateAudience())
                .dateJugement(recours.getDateJugement())
                .decisionFinaleFr(recours.getDecisionFinaleFr())
                .decisionFinaleAr(recours.getDecisionFinaleAr())
                .fichierDecision(recours.getFichierDecision())
                .chambre(recours.getChambre())
                // Compteurs
                .nombreAppelants(recours.getAppelants() != null ? recours.getAppelants().size() : 0)
                .nombreAccuses(recours.getAccuses() != null ? recours.getAccuses().size() : 0)
                .nombreTemoins(recours.getTemoins() != null ? recours.getTemoins().size() : 0)
                .nombrePiecesJointes(recours.getPiecesJointes() != null ? recours.getPiecesJointes().size() : 0)

                // ✅✅✅ أضف هذه الحقول (أعضاء الهيئة القضائية) ✅✅✅
                .presidentNom(recours.getPresidentNom())
                .membre1Nom(recours.getMembre1Nom())
                .membre2Nom(recours.getMembre2Nom())
                .representantMinistere(recours.getRepresentantMinistere())
                .greffierAudience(recours.getGreffierAudience())

                .build();
    }

    private RecoursResponse toDetailedResponse(Recours recours) {
        RecoursResponse response = toResponse(recours);

        response.setAppelants(recours.getAppelants().stream()
                .map(this::toAppelantResponse).collect(Collectors.toList()));
        response.setAccuses(recours.getAccuses().stream()
                .map(this::toAccuseResponse).collect(Collectors.toList()));
        response.setTemoins(recours.getTemoins().stream()
                .map(this::toTemoinResponse).collect(Collectors.toList()));
        response.setPiecesJointes(recours.getPiecesJointes().stream()
                .map(this::toPieceJointeResponse).collect(Collectors.toList()));
        response.setHistorique(historiqueStatutRepository.findByRecoursOrderByDateModificationDesc(recours).stream()
                .map(this::toHistoriqueResponse).collect(Collectors.toList()));

        return response;
    }

    private RecoursResponse.AppelantResponseDTO toAppelantResponse(Appelant appelant) {
        return RecoursResponse.AppelantResponseDTO.builder()
                .id(appelant.getId()).cin(appelant.getCin())
                .nomFr(appelant.getNomFr()).prenomFr(appelant.getPrenomFr())
                .nomAr(appelant.getNomAr()).prenomAr(appelant.getPrenomAr())
                .qualiteFr(appelant.getQualiteFr()).qualiteAr(appelant.getQualiteAr())
                .estMajeur(appelant.getEstMajeur()).build();
    }

    private RecoursResponse.AccuseResponseDTO toAccuseResponse(Accuse accuse) {
        return RecoursResponse.AccuseResponseDTO.builder()
                .id(accuse.getId()).cin(accuse.getCin())
                .nomFr(accuse.getNomFr()).prenomFr(accuse.getPrenomFr())
                .nomAr(accuse.getNomAr()).prenomAr(accuse.getPrenomAr())
                .situationPenale(accuse.getSituationPenale() != null ? accuse.getSituationPenale().name() : null)
                .lieuDetention(accuse.getLieuDetention())
                .qualificationFr(accuse.getQualificationFr()).qualificationAr(accuse.getQualificationAr())
                .estMajeur(accuse.getEstMajeur()).build();
    }

    private RecoursResponse.TemoinResponseDTO toTemoinResponse(Temoin temoin) {
        return RecoursResponse.TemoinResponseDTO.builder()
                .id(temoin.getId()).cin(temoin.getCin())
                .nomFr(temoin.getNomFr()).prenomFr(temoin.getPrenomFr())
                .nomAr(temoin.getNomAr()).prenomAr(temoin.getPrenomAr())
                .professionFr(temoin.getProfessionFr()).professionAr(temoin.getProfessionAr())
                .telephone(temoin.getTelephone())
                .temoignageFr(temoin.getTemoignageFr()).temoignageAr(temoin.getTemoignageAr()).build();
    }

    private RecoursResponse.PieceJointeResponseDTO toPieceJointeResponse(PieceJointe piece) {
        return RecoursResponse.PieceJointeResponseDTO.builder()
                .id(piece.getId()).nomFichier(piece.getNomFichier())
                .typeDocument(piece.getTypeDocument() != null ? piece.getTypeDocument().name() : null)
                .descriptionFr(piece.getDescriptionFr()).descriptionAr(piece.getDescriptionAr())
                .dateUpload(piece.getDateUpload()).tailleOctets(piece.getTailleOctets()).build();
    }

    private RecoursResponse.HistoriqueStatutResponseDTO toHistoriqueResponse(HistoriqueStatut historique) {
        return RecoursResponse.HistoriqueStatutResponseDTO.builder()
                .id(historique.getId())
                .ancienStatut(historique.getAncienStatut()).nouveauStatut(historique.getNouveauStatut())
                .modifieParNom(historique.getModifiePar() != null ? historique.getModifiePar().getFullNameFr() : "Système")
                .dateModification(historique.getDateModification())
                .commentaireFr(historique.getCommentaireFr()).commentaireAr(historique.getCommentaireAr()).build();
    }

    // ============================================================
    // Méthodes supplémentaires
    // ============================================================

    // ✅ AJOUT : @Transactional(readOnly = true)
    @Transactional(readOnly = true)
    public RecoursResponse findByNumeroRecours(String numero) {
        Recours recours = recoursRepository.findByNumeroRecours(numero)
                .orElseThrow(() -> new ResourceNotFoundException("Recours non trouvé: " + numero));
        return toDetailedResponse(recours);
    }

    // ✅ AJOUT : @Transactional(readOnly = true)
    @Transactional(readOnly = true)
    public RecoursResponse findDetailedById(Long id) {
        return toDetailedResponse(findEntityById(id));
    }

    public long countAll() { return recoursRepository.count(); }

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
                .map(this::toResponse).collect(Collectors.toList());
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
        if (numero != null && !numero.isEmpty()) {
            return recoursRepository.findByNumeroRecours(numero)
                    .map(recours -> List.of(toResponse(recours))).orElse(List.of());
        }
        return findAll();
    }

    public Double getDelaiMoyenTraitement() {
        return historiqueStatutRepository.getDelaiMoyenTraitement();
    }
    // ============================================================
    // إحصائيات إضافية - ADDED
    // ============================================================

    public long countAllDossiers() {
        return dossierRepository.count();
    }

    public long countRecoursDepuisJours(int jours) {
        LocalDateTime date = LocalDateTime.now().minusDays(jours);
        return recoursRepository.countByDateDepotAfter(date);
    }

    public Map<String, Long> getRecoursParMois(int nombreMois) {
        Map<String, Long> result = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("MMM");

        for (int i = nombreMois - 1; i >= 0; i--) {
            LocalDateTime debutMois = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime finMois = debutMois.plusMonths(1).minusSeconds(1);

            long count = recoursRepository.countByDateDepotBetween(debutMois, finMois);
            String moisKey = debutMois.format(formatter);
            result.put(moisKey, count);
        }
        return result;
    }

    public long countRecoursEntreDates(java.time.LocalDate debut, java.time.LocalDate fin) {
        LocalDateTime debutDateTime = debut.atStartOfDay();
        LocalDateTime finDateTime = fin.plusDays(1).atStartOfDay().minusSeconds(1);
        return recoursRepository.countByDateDepotBetween(debutDateTime, finDateTime);
    }

    public Map<String, Long> getEvolutionParMois(java.time.LocalDate debut, java.time.LocalDate fin) {
        Map<String, Long> result = new LinkedHashMap<>();
        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("MMM yyyy");

        LocalDate current = debut;
        while (!current.isAfter(fin)) {
            LocalDateTime debutMois = current.withDayOfMonth(1).atStartOfDay();
            LocalDateTime finMois = current.withDayOfMonth(current.lengthOfMonth()).atTime(23, 59, 59);

            long count = recoursRepository.countByDateDepotBetween(debutMois, finMois);
            String moisKey = current.format(formatter);
            result.put(moisKey, count);

            current = current.plusMonths(1);
        }
        return result;
    }
}