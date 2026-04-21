package com.pred.pred_api.model;

import com.pred.pred_api.model.enums.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "utilisateur")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, name = "mot_de_passe")
    private String motDePasse;

    @Column(nullable = false, length = 20)
    private String telephone;

    @Column(unique = true, length = 20)
    private String cin;

    @Column(nullable = false, length = 100, name = "nom_fr")
    private String nomFr;

    @Column(nullable = false, length = 100, name = "prenom_fr")
    private String prenomFr;

    @Column(nullable = false, length = 100, name = "nom_ar")
    private String nomAr;

    @Column(nullable = false, length = 100, name = "prenom_ar")
    private String prenomAr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Genre genre;

    @Column(nullable = false, name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(nullable = false, length = 100, name = "lieu_naissance_fr")
    private String lieuNaissanceFr;

    @Column(nullable = false, length = 100, name = "lieu_naissance_ar")
    private String lieuNaissanceAr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "situation_familiale")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private Boolean actif = true;

    @CreationTimestamp
    @Column(name = "date_inscription", updatable = false)
    private LocalDateTime dateInscription;

    @Column(name = "date_derniere_connexion")
    private LocalDateTime dateDerniereConnexion;

    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Recours> recours = new ArrayList<>();

    @OneToMany(mappedBy = "modifiePar", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HistoriqueStatut> historiqueStatuts = new ArrayList<>();

    @OneToMany(mappedBy = "greffier", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DemandePiece> demandesPieces = new ArrayList<>();

    // ============================================================
    // Méthodes utilitaires - Compatibilité avec l'ancien code
    // ============================================================

    // Pour compatibilité avec AdminService qui utilise getNom() et getPrenom()
    public String getNom() {
        return this.nomFr;
    }

    public String getPrenom() {
        return this.prenomFr;
    }

    public void setNom(String nom) {
        this.nomFr = nom;
    }

    public void setPrenom(String prenom) {
        this.prenomFr = prenom;
    }

    // Méthodes multilingues
    public String getFullNameFr() {
        return this.prenomFr + " " + this.nomFr;
    }

    public String getFullNameAr() {
        return this.prenomAr + " " + this.nomAr;
    }

    public String getFullName(String locale) {
        return "ar".equals(locale) ? getFullNameAr() : getFullNameFr();
    }

    public String getOfficialFullName() {
        return getFullNameFr() + " / " + getFullNameAr();
    }
}