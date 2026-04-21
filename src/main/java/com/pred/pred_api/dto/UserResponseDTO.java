package com.pred.pred_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

    private Long id;
    private String email;
    private String telephone;
    private String cin;
    private String nomFr;
    private String prenomFr;
    private String nomAr;
    private String prenomAr;
    private String genre;
    private LocalDate dateNaissance;
    private String lieuNaissanceFr;
    private String lieuNaissanceAr;
    private String situationFamiliale;
    private String professionFr;
    private String professionAr;
    private String adresseFr;
    private String adresseAr;
    private Boolean estMajeur;
    private String role;
    private Boolean actif;
    private LocalDateTime dateInscription;
    private LocalDateTime dateDerniereConnexion;
    private String fullNameFr;
    private String fullNameAr;

    public String getFullName(String locale) {
        return "ar".equals(locale) ? fullNameAr : fullNameFr;
    }

    public String getOfficialFullName() {
        return fullNameFr + " / " + fullNameAr;
    }
}