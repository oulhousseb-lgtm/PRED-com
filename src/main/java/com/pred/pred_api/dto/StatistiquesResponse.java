package com.pred.pred_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatistiquesResponse {

    // ============================================================
    // Statistiques utilisateurs
    // ============================================================
    private Long totalUtilisateurs;
    private Long totalJusticiables;
    private Long totalAvocats;
    private Long totalGreffiers;
    private Long totalMagistrats;
    private Long totalAdmins;
    private Long utilisateursActifs;
    private Long utilisateursInactifs;

    // ============================================================
    // Statistiques recours
    // ============================================================
    private Long totalRecours;
    private Map<String, Long> recoursParStatut;
    private Map<String, Long> recoursParType;
    private Map<String, Long> recoursParMois;

    // ============================================================
    // Statistiques de performance
    // ============================================================
    private Double delaiMoyenTraitementJours;
    private Long recoursEnAttente;
    private Long recoursClotures;

    // ============================================================
    // Statistiques des pièces
    // ============================================================
    private Long totalPiecesJointes;
    private Long tailleTotaleOctets;
    private Double tailleMoyenneMo;

    // ============================================================
    // Période
    // ============================================================
    private String dateDebut;
    private String dateFin;
}