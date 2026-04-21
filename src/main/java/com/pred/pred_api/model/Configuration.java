package com.pred.pred_api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Configuration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String cle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String valeur;

    @Column(columnDefinition = "TEXT", name = "description_fr")
    private String descriptionFr;

    @Column(columnDefinition = "TEXT", name = "description_ar")
    private String descriptionAr;

    @UpdateTimestamp
    @Column(name = "date_modification")
    private LocalDateTime dateModification;
}