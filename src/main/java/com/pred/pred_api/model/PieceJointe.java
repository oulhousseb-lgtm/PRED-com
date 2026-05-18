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

    // ============================================================
    // Métadonnées du fichier
    // ============================================================

    @Column(nullable = false, length = 255, name = "nom_fichier")
    private String nomFichier;

    @Column(nullable = false, length = 255, name = "chemin_fichier")
    private String cheminFichier;

    @Column(length = 500, name = "chemin_stockage")
    private String cheminStockage;  // Chemin hiérarchique: ANNEE/CODE/NUMERO/

    // ============================================================
    // Type et descriptions bilingues
    // ============================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, name = "type_document")
    private TypeDocument typeDocument;

    @Column(length = 255, name = "description_fr")
    private String descriptionFr;

    @Column(length = 255, name = "description_ar")
    private String descriptionAr;

    // ============================================================
    // Informations techniques
    // ============================================================

    @CreationTimestamp
    @Column(name = "date_upload", updatable = false)
    private LocalDateTime dateUpload;

    @Column(name = "taille_octets", nullable = false)
    private Long tailleOctets;

    @Column(length = 64, name = "hash_sha256", nullable = false)
    private String hashSha256;

    // ============================================================
    // Méthodes utilitaires
    // ============================================================

    /**
     * Retourne la taille formatée (Ko, Mo)
     */
    public String getTailleFormatee() {
        if (tailleOctets == null) return "0 o";
        if (tailleOctets < 1024) return tailleOctets + " o";
        if (tailleOctets < 1048576) return String.format("%.1f Ko", tailleOctets / 1024.0);
        return String.format("%.2f Mo", tailleOctets / 1048576.0);
    }

    /**
     * Retourne le chemin complet du fichier
     */
    public String getCheminComplet() {
        if (cheminStockage != null && !cheminStockage.isEmpty()) {
            return cheminStockage + nomFichier;
        }
        return cheminFichier;
    }

    /**
     * Vérifie si le fichier est un PDF
     */
    public boolean isPDF() {
        return nomFichier != null && nomFichier.toLowerCase().endsWith(".pdf");
    }

    /**
     * Retourne l'extension du fichier
     */
    public String getExtension() {
        if (nomFichier == null) return "";
        int lastDot = nomFichier.lastIndexOf('.');
        return lastDot > 0 ? nomFichier.substring(lastDot).toLowerCase() : "";
    }

    @PrePersist
    protected void onCreate() {
        if (dateUpload == null) {
            dateUpload = LocalDateTime.now();
        }
    }
}