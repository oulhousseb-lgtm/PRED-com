package com.pred.pred_api.dto;

import lombok.Data;

@Data
public class ChangerStatutRequest {
    private String nouveauStatut;
    private String commentaireFr;
    private String commentaireAr;
    private String dateAudience;
    // ✅ الحقول الجديدة لأعضاء الهيئة
    private String presidentNom;
    private String membre1Nom;
    private String membre2Nom;
    private String representantMinistere;
    private String greffierAudience;
    private String decisionFinaleFr;
    private String decisionFinaleAr;
}