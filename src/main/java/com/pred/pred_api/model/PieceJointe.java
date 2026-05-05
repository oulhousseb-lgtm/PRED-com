package com.pred.pred_api.model;

import com.pred.pred_api.model.enums.TypeDocument;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "piece_jointe")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PieceJointe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recours_id", nullable = false)
    private Recours recours;

    @Column(nullable = false, name = "nom_fichier")
    private String nomFichier;

    @Column(nullable = false, name = "chemin_fichier")
    private String cheminFichier;

    // ============================================================
    // MODIFICATION : PieceJointe.java - Ajouter après chemin_fichier
    // ============================================================
    @Column(length = 500, name = "chemin_stockage")
    private String cheminStockage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "type_document")
    private TypeDocument typeDocument;

    @Column(length = 255, name = "description_fr")
    private String descriptionFr;

    @Column(length = 255, name = "description_ar")
    private String descriptionAr;

    @CreationTimestamp
    @Column(name = "date_upload", updatable = false)
    private LocalDateTime dateUpload;

    @Column(name = "taille_octets")
    private Long tailleOctets;

    @Column(length = 64, name = "hash_sha256")
    private String hashSha256;
}