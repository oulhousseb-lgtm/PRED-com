package com.pred.pred_api.dto;

import lombok.Data;

@Data
public class ChangePasswordRequest {

    private String ancienMotDePasse;
    private String nouveauMotDePasse;
    private String confirmationMotDePasse;
}