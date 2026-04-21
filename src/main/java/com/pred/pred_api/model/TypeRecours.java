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
@Table(name = "type_recours")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeRecours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String categorie;

    @Column(nullable = false, length = 200, name = "libelle_fr")
    private String libelleFr;

    @Column(nullable = false, length = 200, name = "libelle_ar")
    private String libelleAr;

    @Column(columnDefinition = "TEXT", name = "description_fr")
    private String descriptionFr;

    @Column(columnDefinition = "TEXT", name = "description_ar")
    private String descriptionAr;

    @Column(nullable = false)
    private Boolean actif = true;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @OneToMany(mappedBy = "typeRecours", fetch = FetchType.LAZY)
    private List<Recours> recours = new ArrayList<>();

    // Méthodes utilitaires
    public String getLibelle(String locale) {
        return "ar".equals(locale) ? libelleAr : libelleFr;
    }

    public String getDescription(String locale) {
        return "ar".equals(locale) ? descriptionAr : descriptionFr;
    }
}