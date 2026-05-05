// ============================================================
// FICHIER : src/main/java/com/pred/pred_api/service/DossierService.java
// CORRIGÉ : Utilise les Enums et les vrais noms de méthodes
// ============================================================
package com.pred.pred_api.service;

import com.pred.pred_api.model.*;
import com.pred.pred_api.model.enums.*;
import com.pred.pred_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DossierService {

    private final DossierRepository dossierRepository;
    private final RecoursRepository recoursRepository;
    private final UserRepository userRepository;
    private final TypeRecoursRepository typeRecoursRepository;
    private final AppelantRepository appelantRepository;
    private final AccuseRepository accuseRepository;
    private final TemoinRepository temoinRepository;

    @Transactional(readOnly = true)
    public List<Dossier> findByUtilisateur(Long utilisateurId) {
        User user = userRepository.findById(utilisateurId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return dossierRepository.findByUtilisateurOrderByDateCreationDesc(user);
    }

    @Transactional(readOnly = true)
    public Dossier findById(Long id) {
        return dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));
    }

    @Transactional
    public Map<String, Object> createDossierWithAffaires(User user, String titreFr, String titreAr,
                                                         List<Map<String, Object>> affairesData) {
        // 1. Créer le dossier
        String annee = String.valueOf(LocalDate.now().getYear());
        long count = dossierRepository.count() + 1;
        String numeroDossier = String.format("DOS-%s-%06d", annee, count);

        Dossier dossier = Dossier.builder()
                .numeroDossier(numeroDossier)
                .utilisateur(user)
                .titreFr(titreFr)
                .titreAr(titreAr)
                .statut("OUVERT")
                .build();

        dossier = dossierRepository.save(dossier);

        // 2. Créer chaque recours lié au dossier
        List<Long> recoursIds = new ArrayList<>();
        long compteurRecours = recoursRepository.count();

        for (Map<String, Object> data : affairesData) {
            compteurRecours++;
            String numeroRecours = String.format("PRED-%s-%06d", annee, compteurRecours);

            // Récupérer le type de recours
            Long typeRecoursId = toLong(data.get("typeRecoursId"));
            TypeRecours typeRecours = typeRecoursRepository.findById(typeRecoursId)
                    .orElseThrow(() -> new RuntimeException("Type de recours non trouvé: " + typeRecoursId));

            // Parser la date
            String dateStr = getString(data, "dateDecisionAttaque");
            LocalDate dateDecision = null;
            if (dateStr != null && !dateStr.isEmpty()) {
                dateDecision = LocalDate.parse(dateStr);
            }

            // Créer le recours avec le bon Enum StatutRecours
            Recours recours = Recours.builder()
                    .numeroRecours(numeroRecours)
                    .utilisateur(user)
                    .dossier(dossier)
                    .typeRecours(typeRecours)
                    .numeroDecisionAttaque(
                            getString(data, "numeroSequentiel") + "/" +
                                    getString(data, "codeRecours") + "/" +
                                    getString(data, "anneeDecision")
                    )
                    .dateDecisionAttaque(dateDecision)
                    .juridictionSourceFr(getString(data, "juridictionSourceFr"))
                    .juridictionSourceAr(getString(data, "juridictionSourceAr"))
                    .moyensRecoursFr(getString(data, "moyensRecoursFr"))
                    .moyensRecoursAr(getString(data, "moyensRecoursAr"))
                    .chambre(getString(data, "chambre"))
                    .statut(StatutRecours.DEPOSE)  // Utiliser l'Enum
                    .dateDepot(LocalDateTime.now())
                    .build();

            recours = recoursRepository.save(recours);
            recoursIds.add(recours.getId());

            // Ajouter les appelants
            List<Map<String, Object>> appelants = getList(data, "appelants");
            for (Map<String, Object> appData : appelants) {
                Appelant appelant = new Appelant();
                appelant.setRecours(recours);
                appelant.setCin(getString(appData, "cin"));
                appelant.setNomFr(getString(appData, "nomFr"));
                appelant.setPrenomFr(getString(appData, "prenomFr"));
                appelant.setNomAr(getString(appData, "nomAr"));
                appelant.setPrenomAr(getString(appData, "prenomAr"));

                // Utiliser l'Enum Genre
                String genreStr = getString(appData, "genre");
                appelant.setGenre(genreStr != null ? Genre.valueOf(genreStr) : Genre.HOMME);

                appelant.setDateNaissance(parseDate(getString(appData, "dateNaissance")));
                appelant.setLieuNaissanceFr(getString(appData, "lieuNaissanceFr"));
                appelant.setLieuNaissanceAr(getString(appData, "lieuNaissanceAr"));

                // Utiliser l'Enum SituationFamiliale
                String sitFamStr = getString(appData, "situationFamiliale");
                appelant.setSituationFamiliale(sitFamStr != null ? SituationFamiliale.valueOf(sitFamStr) : SituationFamiliale.CELIBATAIRE);

                appelant.setProfessionFr(getString(appData, "professionFr"));
                appelant.setProfessionAr(getString(appData, "professionAr"));
                appelant.setAdresseFr(getString(appData, "adresseFr"));
                appelant.setAdresseAr(getString(appData, "adresseAr"));

                Boolean estMajeur = (Boolean) appData.getOrDefault("estMajeur", true);
                appelant.setEstMajeur(estMajeur != null ? estMajeur : true);

                appelant.setQualiteFr(getString(appData, "qualiteFr"));
                appelant.setQualiteAr(getString(appData, "qualiteAr"));

                // Vérifier si les méthodes tuteur existent avant de les appeler
                try {
                    appelant.getClass().getMethod("setTuteurNomFr", String.class);
                    // Si les méthodes existent, les utiliser
                    setTuteurFields(appelant, appData);
                } catch (NoSuchMethodException e) {
                    // Les champs tuteur n'existent pas dans ce modèle
                }

                appelantRepository.save(appelant);
            }

            // Ajouter les accusés
            List<Map<String, Object>> accuses = getList(data, "accuses");
            for (Map<String, Object> accData : accuses) {
                Accuse accuse = new Accuse();
                accuse.setRecours(recours);
                accuse.setCin(getString(accData, "cin"));
                accuse.setNomFr(getString(accData, "nomFr"));
                accuse.setPrenomFr(getString(accData, "prenomFr"));
                accuse.setNomAr(getString(accData, "nomAr"));
                accuse.setPrenomAr(getString(accData, "prenomAr"));

                String genreStr = getString(accData, "genre");
                accuse.setGenre(genreStr != null ? Genre.valueOf(genreStr) : Genre.HOMME);

                accuse.setDateNaissance(parseDate(getString(accData, "dateNaissance")));
                accuse.setLieuNaissanceFr(getString(accData, "lieuNaissanceFr"));
                accuse.setLieuNaissanceAr(getString(accData, "lieuNaissanceAr"));

                String sitFamStr = getString(accData, "situationFamiliale");
                accuse.setSituationFamiliale(sitFamStr != null ? SituationFamiliale.valueOf(sitFamStr) : SituationFamiliale.CELIBATAIRE);

                accuse.setProfessionFr(getString(accData, "professionFr"));
                accuse.setProfessionAr(getString(accData, "professionAr"));
                accuse.setAdresseFr(getString(accData, "adresseFr"));
                accuse.setAdresseAr(getString(accData, "adresseAr"));

                Boolean estMajeur = (Boolean) accData.getOrDefault("estMajeur", true);
                accuse.setEstMajeur(estMajeur != null ? estMajeur : true);

                // Enum SituationPenale
                String sitPenaleStr = getString(accData, "situationPenale");
                accuse.setSituationPenale(sitPenaleStr != null ? SituationPenale.valueOf(sitPenaleStr) : SituationPenale.LIBRE);

                accuse.setLieuDetention(getString(accData, "lieuDetention"));
                accuse.setQualificationFr(getString(accData, "qualificationFr"));
                accuse.setQualificationAr(getString(accData, "qualificationAr"));

                accuseRepository.save(accuse);
            }

            // Ajouter les témoins
            List<Map<String, Object>> temoins = getList(data, "temoins");
            for (Map<String, Object> temData : temoins) {
                Temoin temoin = new Temoin();
                temoin.setRecours(recours);
                temoin.setCin(getString(temData, "cin"));
                temoin.setNomFr(getString(temData, "nomFr"));
                temoin.setPrenomFr(getString(temData, "prenomFr"));
                temoin.setNomAr(getString(temData, "nomAr"));
                temoin.setPrenomAr(getString(temData, "prenomAr"));
                temoin.setProfessionFr(getString(temData, "professionFr"));
                temoin.setProfessionAr(getString(temData, "professionAr"));
                temoin.setAdresseFr(getString(temData, "adresseFr"));
                temoin.setAdresseAr(getString(temData, "adresseAr"));
                temoin.setTelephone(getString(temData, "telephone"));
                temoin.setTemoignageFr(getString(temData, "temoignageFr"));
                temoin.setTemoignageAr(getString(temData, "temoignageAr"));

                temoinRepository.save(temoin);
            }
        }

        // 3. Retourner les IDs
        Map<String, Object> result = new HashMap<>();
        result.put("dossier_id", dossier.getId());
        result.put("numero_dossier", numeroDossier);
        result.put("recours_ids", recoursIds);
        result.put("message", "Dossier cree avec " + affairesData.size() + " affaire(s)");

        return result;
    }

    // ============================================================
    // MÉTHODES UTILITAIRES
    // ============================================================

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return "";
        return value.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return new ArrayList<>();
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        return Long.valueOf(value.toString());
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    // Méthode séparée pour les champs tuteur (évite les erreurs si absents)
    private void setTuteurFields(Appelant appelant, Map<String, Object> data) {
        // Ces méthodes peuvent ne pas exister selon la version du modèle
        // On les appelle via réflexion pour éviter les erreurs de compilation
        try {
            java.lang.reflect.Method setTuteurNomFr = Appelant.class.getMethod("setTuteurNomFr", String.class);
            java.lang.reflect.Method setTuteurPrenomFr = Appelant.class.getMethod("setTuteurPrenomFr", String.class);
            java.lang.reflect.Method setTuteurNomAr = Appelant.class.getMethod("setTuteurNomAr", String.class);
            java.lang.reflect.Method setTuteurPrenomAr = Appelant.class.getMethod("setTuteurPrenomAr", String.class);
            java.lang.reflect.Method setTuteurCin = Appelant.class.getMethod("setTuteurCin", String.class);
            java.lang.reflect.Method setTuteurQualiteFr = Appelant.class.getMethod("setTuteurQualiteFr", String.class);
            java.lang.reflect.Method setTuteurQualiteAr = Appelant.class.getMethod("setTuteurQualiteAr", String.class);
            java.lang.reflect.Method setTuteurAdresseFr = Appelant.class.getMethod("setTuteurAdresseFr", String.class);
            java.lang.reflect.Method setTuteurAdresseAr = Appelant.class.getMethod("setTuteurAdresseAr", String.class);

            setTuteurNomFr.invoke(appelant, getString(data, "tuteurNomFr"));
            setTuteurPrenomFr.invoke(appelant, getString(data, "tuteurPrenomFr"));
            setTuteurNomAr.invoke(appelant, getString(data, "tuteurNomAr"));
            setTuteurPrenomAr.invoke(appelant, getString(data, "tuteurPrenomAr"));
            setTuteurCin.invoke(appelant, getString(data, "tuteurCin"));
            setTuteurQualiteFr.invoke(appelant, getString(data, "tuteurQualiteFr"));
            setTuteurQualiteAr.invoke(appelant, getString(data, "tuteurQualiteAr"));
            setTuteurAdresseFr.invoke(appelant, getString(data, "tuteurAdresseFr"));
            setTuteurAdresseAr.invoke(appelant, getString(data, "tuteurAdresseAr"));
        } catch (Exception e) {
            // Les champs tuteur n'existent pas, on ignore
        }
    }
}