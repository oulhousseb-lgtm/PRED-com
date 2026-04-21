package com.pred.pred_api.model;

import com.pred.pred_api.model.enums.StatutDemandePiece;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "demande_piece")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandePiece {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recours_id", nullable = false)
    private Recours recours;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "greffier_id", nullable = false)
    private User greffier;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    private Integer delaiJour = 15;

    @CreationTimestamp
    @Column(name = "date_envoi", updatable = false)
    private LocalDateTime dateEnvoi;

    @Column(name = "date_expiration")
    private LocalDateTime dateExpiration;

    @Enumerated(EnumType.STRING)
    private StatutDemandePiece statut = StatutDemandePiece.EN_ATTENTE;
}