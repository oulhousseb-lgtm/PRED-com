package com.pred.pred_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String type = "Bearer";
    private Long id;
    private String email;
    private String role;
    private String nomFr;
    private String prenomFr;
    private String nomAr;
    private String prenomAr;

    public String getFullNameFr() {
        return prenomFr + " " + nomFr;
    }

    public String getFullNameAr() {
        return prenomAr + " " + nomAr;
    }
}