package com.pred.pred_api.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class RecoursRequest {

    // ============================================================
    // Informations du recours principal
    // ============================================================
    private Long typeRecoursId;

    // Décision attaquée
    private String numeroDecisionAttaque;
    private LocalDate dateDecisionAttaque;
    private String juridictionSourceFr;
    private String juridictionSourceAr;

    // Moyens de recours
    private String moyensRecoursFr;
    private String moyensRecoursAr;

    // Métadonnées
    private String chambre;

    // ============================================================
    // Listes des parties
    // ============================================================
    private List<AppelantDTO> appelants;
    private List<AccuseDTO> accuses;
    private List<TemoinDTO> temoins;

    // ============================================================
    // DTOs internes
    // ============================================================

    @Data
    public static class AppelantDTO {
        // Si l'appelant est un utilisateur enregistré
        private Long utilisateurId;

        // Identité
        private String cin;
        private String nomFr;
        private String prenomFr;
        private String nomAr;
        private String prenomAr;

        // Informations personnelles
        private String genre;
        private LocalDate dateNaissance;
        private String lieuNaissanceFr;
        private String lieuNaissanceAr;
        private String situationFamiliale;
        private String professionFr;
        private String professionAr;
        private String adresseFr;
        private String adresseAr;
        private Boolean estMajeur = true;

        // Qualité dans le recours
        private String qualiteFr;
        private String qualiteAr;
    }

    @Data
    public static class AccuseDTO {
        // Identité
        private String cin;
        private String nomFr;
        private String prenomFr;
        private String nomAr;
        private String prenomAr;

        // Informations personnelles
        private String genre;
        private LocalDate dateNaissance;
        private String lieuNaissanceFr;
        private String lieuNaissanceAr;
        private String situationFamiliale;
        private String professionFr;
        private String professionAr;
        private String adresseFr;
        private String adresseAr;
        private Boolean estMajeur = true;

        // Situation pénale
        private String situationPenale = "LIBRE";
        private String lieuDetention;

        // Qualification pénale
        private String qualificationFr;
        private String qualificationAr;
    }

    @Data
    public static class TemoinDTO {
        // Identité
        private String cin;
        private String nomFr;
        private String prenomFr;
        private String nomAr;
        private String prenomAr;

        // Informations complémentaires
        private String professionFr;
        private String professionAr;
        private String adresseFr;
        private String adresseAr;
        private String telephone;

        // Témoignage
        private String temoignageFr;
        private String temoignageAr;
    }
}