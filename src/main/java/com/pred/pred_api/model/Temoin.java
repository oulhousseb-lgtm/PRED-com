package com.pred.pred_api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "temoin")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Temoin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recours_id", nullable = false)
    private Recours recours;

    // ============================================================
    // Identité
    // ============================================================
    @Column(length = 20)
    private String cin;

    @Column(nullable = false, length = 100, name = "nom_fr")
    private String nomFr;

    @Column(nullable = false, length = 100, name = "prenom_fr")
    private String prenomFr;

    @Column(nullable = false, length = 100, name = "nom_ar")
    private String nomAr;

    @Column(nullable = false, length = 100, name = "prenom_ar")
    private String prenomAr;

    // ============================================================
    // Informations complémentaires
    // ============================================================
    @Column(length = 100, name = "profession_fr")
    private String professionFr;

    @Column(length = 100, name = "profession_ar")
    private String professionAr;

    @Column(columnDefinition = "TEXT", name = "adresse_fr")
    private String adresseFr;

    @Column(columnDefinition = "TEXT", name = "adresse_ar")
    private String adresseAr;

    @Column(length = 20)
    private String telephone;

    // ============================================================
    // Témoignage
    // ============================================================
    @Column(columnDefinition = "TEXT", name = "temoignage_fr")
    private String temoignageFr;

    @Column(columnDefinition = "TEXT", name = "temoignage_ar")
    private String temoignageAr;

    // ============================================================
    // Méthodes utilitaires
    // ============================================================
    public String getFullNameFr() {
        return prenomFr + " " + nomFr;
    }

    public String getFullNameAr() {
        return prenomAr + " " + nomAr;
    }
}