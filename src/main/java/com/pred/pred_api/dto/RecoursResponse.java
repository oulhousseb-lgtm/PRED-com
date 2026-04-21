package com.pred.pred_api.dto;

import com.pred.pred_api.model.enums.StatutRecours;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecoursResponse {

    // ============================================================
    // Informations de base
    // ============================================================
    private Long id;
    private String numeroRecours;

    // ============================================================
    // Type de recours
    // ============================================================
    private Long typeRecoursId;
    private String typeRecoursCode;
    private String typeRecoursLibelleFr;
    private String typeRecoursLibelleAr;
    private String typeRecoursCategorie;

    // ============================================================
    // Déposant
    // ============================================================
    private Long deposantId;
    private String deposantNomFr;
    private String deposantPrenomFr;
    private String deposantNomAr;
    private String deposantPrenomAr;
    private String deposantEmail;

    // ============================================================
    // Décision attaquée
    // ============================================================
    private String numeroDecisionAttaque;
    private LocalDate dateDecisionAttaque;
    private String juridictionSourceFr;
    private String juridictionSourceAr;

    // ============================================================
    // Moyens de recours
    // ============================================================
    private String moyensRecoursFr;
    private String moyensRecoursAr;
    private String fichierMoyens;

    // ============================================================
    // Statut et dates
    // ============================================================
    private StatutRecours statut;
    private LocalDateTime dateDepot;
    private LocalDate dateAudience;
    private LocalDate dateJugement;

    // ============================================================
    // Décision finale
    // ============================================================
    private String decisionFinaleFr;
    private String decisionFinaleAr;
    private String fichierDecision;

    // ============================================================
    // Métadonnées
    // ============================================================
    private String chambre;

    // ============================================================
    // Statistiques des parties
    // ============================================================
    private Integer nombreAppelants;
    private Integer nombreAccuses;
    private Integer nombreTemoins;
    private Integer nombrePiecesJointes;

    // ============================================================
    // Listes détaillées (optionnel, pour consultation détaillée)
    // ============================================================
    private List<AppelantResponseDTO> appelants;
    private List<AccuseResponseDTO> accuses;
    private List<TemoinResponseDTO> temoins;
    private List<PieceJointeResponseDTO> piecesJointes;
    private List<HistoriqueStatutResponseDTO> historique;

    // ============================================================
    // DTOs internes pour les réponses détaillées
    // ============================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppelantResponseDTO {
        private Long id;
        private String cin;
        private String nomFr;
        private String prenomFr;
        private String nomAr;
        private String prenomAr;
        private String qualiteFr;
        private String qualiteAr;
        private Boolean estMajeur;

        public String getFullNameFr() {
            return prenomFr + " " + nomFr;
        }

        public String getFullNameAr() {
            return prenomAr + " " + nomAr;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccuseResponseDTO {
        private Long id;
        private String cin;
        private String nomFr;
        private String prenomFr;
        private String nomAr;
        private String prenomAr;
        private String situationPenale;
        private String lieuDetention;
        private String qualificationFr;
        private String qualificationAr;
        private Boolean estMajeur;

        public String getFullNameFr() {
            return prenomFr + " " + nomFr;
        }

        public String getFullNameAr() {
            return prenomAr + " " + nomAr;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemoinResponseDTO {
        private Long id;
        private String cin;
        private String nomFr;
        private String prenomFr;
        private String nomAr;
        private String prenomAr;
        private String professionFr;
        private String professionAr;
        private String telephone;
        private String temoignageFr;
        private String temoignageAr;

        public String getFullNameFr() {
            return prenomFr + " " + nomFr;
        }

        public String getFullNameAr() {
            return prenomAr + " " + nomAr;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PieceJointeResponseDTO {
        private Long id;
        private String nomFichier;
        private String typeDocument;
        private String descriptionFr;
        private String descriptionAr;
        private LocalDateTime dateUpload;
        private Long tailleOctets;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoriqueStatutResponseDTO {
        private Long id;
        private String ancienStatut;
        private String nouveauStatut;
        private String modifieParNom;
        private LocalDateTime dateModification;
        private String commentaireFr;
        private String commentaireAr;
    }
}