// ============================================================
// FICHIER : src/main/java/com/pred/pred_api/model/Dossier.java
// ============================================================
package com.pred.pred_api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dossier")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50, name = "numero_dossier")
    private String numeroDossier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private User utilisateur;

    // ============================================================
    // Titres bilingues
    // ============================================================
    @Column(length = 200, name = "titre_fr")
    private String titreFr;

    @Column(length = 200, name = "titre_ar")
    private String titreAr;

    // ============================================================
    // Descriptions bilingues
    // ============================================================
    @Column(columnDefinition = "TEXT", name = "description_fr")
    private String descriptionFr;

    @Column(columnDefinition = "TEXT", name = "description_ar")
    private String descriptionAr;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @Column(nullable = false, length = 20)
    private String statut = "OUVERT";

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Recours> recours = new ArrayList<>();

    // ============================================================
    // Méthodes utilitaires bilingues
    // ============================================================
    public String getTitre(String locale) {
        return "ar".equals(locale) ? titreAr : titreFr;
    }

    public String getDescription(String locale) {
        return "ar".equals(locale) ? descriptionAr : descriptionFr;
    }

    public String getStatutLabel(String locale) {
        switch (statut) {
            case "OUVERT": return "ar".equals(locale) ? "مفتوح" : "Ouvert";
            case "CLOTURE": return "ar".equals(locale) ? "مغلق" : "Clôturé";
            case "ARCHIVE": return "ar".equals(locale) ? "مؤرشفة" : "Archivé";
            default: return statut;
        }
    }
}