package com.pred.pred_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeRecoursResponse {

    private Long id;
    private String code;
    private String categorie;
    private String libelleFr;
    private String libelleAr;
    private String descriptionFr;
    private String descriptionAr;
    private Boolean actif;
    private LocalDateTime dateCreation;

    // Statistiques
    private Long nombreRecours;

    public String getLibelle(String locale) {
        return "ar".equals(locale) ? libelleAr : libelleFr;
    }

    public String getDescription(String locale) {
        return "ar".equals(locale) ? descriptionAr : descriptionFr;
    }
}