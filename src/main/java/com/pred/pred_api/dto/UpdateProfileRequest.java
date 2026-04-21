package com.pred.pred_api.dto;

import com.pred.pred_api.model.enums.Genre;
import com.pred.pred_api.model.enums.SituationFamiliale;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {

    private String telephone;
    private Genre genre;
    private LocalDate dateNaissance;
    private String lieuNaissanceFr;
    private String lieuNaissanceAr;
    private SituationFamiliale situationFamiliale;
    private String professionFr;
    private String professionAr;
    private String adresseFr;
    private String adresseAr;
}