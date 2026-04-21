package com.pred.pred_api.model;

import com.pred.pred_api.model.enums.Genre;
import com.pred.pred_api.model.enums.SituationFamiliale;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "appelant")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appelant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recours_id", nullable = false)
    private Recours recours;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private User utilisateur;

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
    // Informations personnelles
    // ============================================================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Genre genre;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(length = 100, name = "lieu_naissance_fr")
    private String lieuNaissanceFr;

    @Column(length = 100, name = "lieu_naissance_ar")
    private String lieuNaissanceAr;

    @Enumerated(EnumType.STRING)
    @Column(name = "situation_familiale")
    private SituationFamiliale situationFamiliale;

    @Column(length = 100, name = "profession_fr")
    private String professionFr;

    @Column(length = 100, name = "profession_ar")
    private String professionAr;

    @Column(columnDefinition = "TEXT", name = "adresse_fr")
    private String adresseFr;

    @Column(columnDefinition = "TEXT", name = "adresse_ar")
    private String adresseAr;

    @Column(nullable = false, name = "est_majeur")
    private Boolean estMajeur = true;

    // ============================================================
    // Qualité dans le recours
    // ============================================================
    @Column(length = 100, name = "qualite_fr")
    private String qualiteFr;

    @Column(length = 100, name = "qualite_ar")
    private String qualiteAr;

    // ============================================================
    // Méthodes utilitaires
    // ============================================================
    public String getFullNameFr() {
        return prenomFr + " " + nomFr;
    }

    public String getFullNameAr() {
        return prenomAr + " " + nomAr;
    }

    public String getFullName(String locale) {
        return "ar".equals(locale) ? getFullNameAr() : getFullNameFr();
    }
}