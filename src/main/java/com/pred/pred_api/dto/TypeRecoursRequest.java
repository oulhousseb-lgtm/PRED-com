package com.pred.pred_api.dto;

import lombok.Data;

@Data
public class TypeRecoursRequest {

    private String code;
    private String categorie;
    private String libelleFr;
    private String libelleAr;
    private String descriptionFr;
    private String descriptionAr;
    private Boolean actif = true;
}