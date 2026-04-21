package com.pred.pred_api.model;

import com.pred.pred_api.model.enums.TypeNotification;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private User utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recours_id")
    private Recours recours;

    @Column(nullable = false, length = 200, name = "titre_fr")
    private String titreFr;

    @Column(nullable = false, columnDefinition = "TEXT", name = "message_fr")
    private String messageFr;

    @Column(nullable = false, length = 200, name = "titre_ar")
    private String titreAr;

    @Column(nullable = false, columnDefinition = "TEXT", name = "message_ar")
    private String messageAr;

    @Column(nullable = false)
    private Boolean lu = false;

    @CreationTimestamp
    @Column(name = "date_envoi", updatable = false)
    private LocalDateTime dateEnvoi;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_notification")
    private TypeNotification typeNotification = TypeNotification.SYSTEME;

    // Méthodes utilitaires
    public String getTitre(String locale) {
        return "ar".equals(locale) ? titreAr : titreFr;
    }

    public String getMessage(String locale) {
        return "ar".equals(locale) ? messageAr : messageFr;
    }
}