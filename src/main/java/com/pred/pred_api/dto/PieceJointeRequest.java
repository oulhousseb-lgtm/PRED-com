package com.pred.pred_api.dto;

import lombok.Data;

@Data
public class PieceJointeRequest {

    private Long recoursId;
    private String typeDocument;
    private String descriptionFr;
    private String descriptionAr;
}