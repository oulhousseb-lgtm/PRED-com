// ============================================================
// FICHIER : src/main/java/com/pred/pred_api/model/Victime.java
// ============================================================
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
@Table(name = "victime")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Victime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recours_id", nullable = false)
    private Recours recours;

    @Column(length = 20)
    private String cin;

    @Column(name = "nom_fr", nullable = false, length = 100)
    private String nomFr;

    @Column(name = "prenom_fr", nullable = false, length = 100)
    private String prenomFr;

    @Column(name = "nom_ar", nullable = false, length = 100)
    private String nomAr;

    @Column(name = "prenom_ar", nullable = false, length = 100)
    private String prenomAr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Genre genre;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "lieu_naissance_fr", length = 100)
    private String lieuNaissanceFr;

    @Column(name = "lieu_naissance_ar", length = 100)
    private String lieuNaissanceAr;

    @Enumerated(EnumType.STRING)
    @Column(name = "situation_familiale", length = 20)
    private SituationFamiliale situationFamiliale;

    @Column(name = "profession_fr", length = 100)
    private String professionFr;

    @Column(name = "profession_ar", length = 100)
    private String professionAr;

    @Column(name = "adresse_fr", columnDefinition = "TEXT")
    private String adresseFr;

    @Column(name = "adresse_ar", columnDefinition = "TEXT")
    private String adresseAr;

    @Column(name = "est_majeur", nullable = false)
    private Boolean estMajeur = true;

    // Stocké comme simple String, pas d'Enum
    @Column(name = "nature_prejudice", length = 50)
    private String naturePrejudice;

    @Column(name = "description_prejudice_fr", columnDefinition = "TEXT")
    private String descriptionPrejudiceFr;

    @Column(name = "description_prejudice_ar", columnDefinition = "TEXT")
    private String descriptionPrejudiceAr;

    // Champs tuteur
    @Column(name = "tuteur_nom_fr", length = 100)
    private String tuteurNomFr;

    @Column(name = "tuteur_prenom_fr", length = 100)
    private String tuteurPrenomFr;

    @Column(name = "tuteur_nom_ar", length = 100)
    private String tuteurNomAr;

    @Column(name = "tuteur_prenom_ar", length = 100)
    private String tuteurPrenomAr;

    @Column(name = "tuteur_cin", length = 20)
    private String tuteurCin;
}