package com.pred.pred_api.model;

import com.pred.pred_api.model.enums.StatutRecours;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recours")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============================================================
    // Référence unique
    // ============================================================
    @Column(nullable = false, unique = true, length = 50, name = "numero_recours")
    private String numeroRecours;

    // ============================================================
    // Relations principales
    // ============================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private User utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_recours_id", nullable = false)
    private TypeRecours typeRecours;

    // ============================================================
    // Décision attaquée
    // ============================================================
    @Column(nullable = false, length = 100, name = "numero_decision_attaque")
    private String numeroDecisionAttaque;

    @Column(nullable = false, name = "date_decision_attaque")
    private LocalDate dateDecisionAttaque;

    @Column(nullable = false, length = 200, name = "juridiction_source_fr")
    private String juridictionSourceFr;

    @Column(nullable = false, length = 200, name = "juridiction_source_ar")
    private String juridictionSourceAr;

    // ============================================================
    // Moyens de recours
    // ============================================================
    @Column(nullable = false, columnDefinition = "TEXT", name = "moyens_recours_fr")
    private String moyensRecoursFr;

    @Column(nullable = false, columnDefinition = "TEXT", name = "moyens_recours_ar")
    private String moyensRecoursAr;

    @Column(length = 500, name = "fichier_moyens")
    private String fichierMoyens;

    // ============================================================
    // Dates clés
    // ============================================================
    @CreationTimestamp
    @Column(name = "date_depot", updatable = false)
    private LocalDateTime dateDepot;

    @Column(name = "date_audience")
    private LocalDate dateAudience;

    @Column(name = "date_jugement")
    private LocalDate dateJugement;

    // ============================================================
    // Statut
    // ============================================================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutRecours statut = StatutRecours.BROUILLON;

    // ============================================================
    // Décision finale
    // ============================================================
    @Column(columnDefinition = "TEXT", name = "decision_finale_fr")
    private String decisionFinaleFr;

    @Column(columnDefinition = "TEXT", name = "decision_finale_ar")
    private String decisionFinaleAr;

    @Column(length = 500, name = "fichier_decision")
    private String fichierDecision;

    // ============================================================
    // Métadonnées
    // ============================================================
    @Column(length = 50)
    private String chambre;

    @Column(columnDefinition = "TEXT", name = "notes_internes")
    private String notesInternes;

    // ============================================================
    // Relations OneToMany
    // ============================================================
    @OneToMany(mappedBy = "recours", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Appelant> appelants = new ArrayList<>();

    @OneToMany(mappedBy = "recours", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Accuse> accuses = new ArrayList<>();

    @OneToMany(mappedBy = "recours", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Temoin> temoins = new ArrayList<>();

    @OneToMany(mappedBy = "recours", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PieceJointe> piecesJointes = new ArrayList<>();

    @OneToMany(mappedBy = "recours", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HistoriqueStatut> historiqueStatuts = new ArrayList<>();

    // ============================================================
    // Méthodes utilitaires
    // ============================================================
    public String getMoyensRecours(String locale) {
        return "ar".equals(locale) ? moyensRecoursAr : moyensRecoursFr;
    }

    public String getJuridictionSource(String locale) {
        return "ar".equals(locale) ? juridictionSourceAr : juridictionSourceFr;
    }

    public String getDecisionFinale(String locale) {
        return "ar".equals(locale) ? decisionFinaleAr : decisionFinaleFr;
    }
}