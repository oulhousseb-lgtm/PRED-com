package com.pred.pred_api.dto;

import com.pred.pred_api.model.enums.TypeNotification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private Long recoursId;
    private String recoursNumero;
    private String titreFr;
    private String messageFr;
    private String titreAr;
    private String messageAr;
    private Boolean lu;
    private LocalDateTime dateEnvoi;
    private TypeNotification typeNotification;

    public String getTitre(String locale) {
        return "ar".equals(locale) ? titreAr : titreFr;
    }

    public String getMessage(String locale) {
        return "ar".equals(locale) ? messageAr : messageFr;
    }
}