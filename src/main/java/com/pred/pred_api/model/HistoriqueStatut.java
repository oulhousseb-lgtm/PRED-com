package com.pred.pred_api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "historique_statut")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoriqueStatut {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recours_id", nullable = false)
    private Recours recours;

    @Column( name = "ancien_statut")
    private String ancienStatut;

    @Column(nullable = false, name = "nouveau_statut")
    private String nouveauStatut;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modifie_par_id", nullable = false)
    private User modifiePar;

    @CreationTimestamp
    @Column(name = "date_modification", updatable = false)
    private LocalDateTime dateModification;

    @Column(columnDefinition = "TEXT", name = "commentaire_fr")
    private String commentaireFr;

    @Column(columnDefinition = "TEXT", name = "commentaire_ar")
    private String commentaireAr;
}