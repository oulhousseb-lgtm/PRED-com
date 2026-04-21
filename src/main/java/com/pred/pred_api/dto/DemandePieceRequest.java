package com.pred.pred_api.dto;

import lombok.Data;

@Data
public class DemandePieceRequest {

    private Long recoursId;
    private String messageFr;
    private String messageAr;
    private Integer delaiJour = 15;
}