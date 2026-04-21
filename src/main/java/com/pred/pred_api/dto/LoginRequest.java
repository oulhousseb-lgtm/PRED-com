package com.pred.pred_api.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String motDePasse;
}
