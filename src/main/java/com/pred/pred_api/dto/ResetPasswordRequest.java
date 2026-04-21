package com.pred.pred_api.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String token;
    private String nouveauMotDePasse;
    private String confirmationMotDePasse;
}