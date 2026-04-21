package com.pred.pred_api.dto;

import com.pred.pred_api.model.enums.Genre;
import com.pred.pred_api.model.enums.SituationFamiliale;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

    // ============================================================
    // Authentification
    // ============================================================
    private String email;
    private String motDePasse;
    private String telephone;

    // ============================================================
    // Identité (Français)
    // ============================================================
    private String cin;  // Optionnel pour les mineurs
    private String nomFr;
    private String prenomFr;

    // ============================================================
    // Identité (Arabe)
    // ============================================================
    private String nomAr;
    private String prenomAr;

    // ============================================================
    // Informations personnelles
    // ============================================================
    private Genre genre;
    private LocalDate dateNaissance;
    private String lieuNaissanceFr;
    private String lieuNaissanceAr;
    private SituationFamiliale situationFamiliale;
    private String professionFr;
    private String professionAr;
    private String adresseFr;
    private String adresseAr;
    private Boolean estMajeur;
}