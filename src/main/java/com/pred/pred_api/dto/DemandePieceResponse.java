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
public class DemandePieceResponse {

    private Long id;
    private Long recoursId;
    private String recoursNumero;
    private Long greffierId;
    private String greffierNom;
    private String greffierPrenom;
    private String messageFr;
    private String messageAr;
    private Integer delaiJour;
    private LocalDateTime dateEnvoi;
    private LocalDateTime dateExpiration;
    private String statut;
    private Boolean estExpiree;
    private Long joursRestants;

    public String getMessage(String locale) {
        return "ar".equals(locale) ? messageAr : messageFr;
    }
}