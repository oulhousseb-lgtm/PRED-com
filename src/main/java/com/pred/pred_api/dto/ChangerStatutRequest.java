package com.pred.pred_api.dto;

import lombok.Data;

@Data
public class ChangerStatutRequest {

    private Long recoursId;
    private String nouveauStatut;
    private String commentaireFr;
    private String commentaireAr;
}