package com.pred.pred_api.repository;

import com.pred.pred_api.model.PieceJointe;
import com.pred.pred_api.model.Recours;
import com.pred.pred_api.model.enums.TypeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PieceJointeRepository extends JpaRepository<PieceJointe, Long> {

    // ============================================================
    // Recherche par recours
    // ============================================================

    List<PieceJointe> findByRecours(Recours recours);

    List<PieceJointe> findByRecoursOrderByDateUploadDesc(Recours recours);

    @Query("SELECT p FROM PieceJointe p WHERE p.recours.id = :recoursId")
    List<PieceJointe> findByRecoursId(@Param("recoursId") Long recoursId);

    // ============================================================
    // Recherche par type de document
    // ============================================================

    List<PieceJointe> findByTypeDocument(TypeDocument typeDocument);

    List<PieceJointe> findByRecoursAndTypeDocument(Recours recours, TypeDocument typeDocument);

    @Query("SELECT p FROM PieceJointe p WHERE p.recours.id = :recoursId AND p.typeDocument = :type")
    List<PieceJointe> findByRecoursIdAndType(@Param("recoursId") Long recoursId,
                                             @Param("type") TypeDocument type);

    // ============================================================
    // Recherche par hash (détection de doublons)
    // ============================================================

    Optional<PieceJointe> findByHashSha256(String hashSha256);

    boolean existsByHashSha256(String hashSha256);

    // ============================================================
    // Statistiques
    // ============================================================

    @Query("SELECT COUNT(p) FROM PieceJointe p WHERE p.recours.id = :recoursId")
    long countByRecoursId(@Param("recoursId") Long recoursId);

    @Query("SELECT SUM(p.tailleOctets) FROM PieceJointe p")
    Long sumTailleOctets();

    @Query("SELECT SUM(p.tailleOctets) FROM PieceJointe p WHERE p.recours.id = :recoursId")
    Long sumTailleOctetsByRecoursId(@Param("recoursId") Long recoursId);

    @Query("SELECT p.typeDocument, COUNT(p), SUM(p.tailleOctets) FROM PieceJointe p GROUP BY p.typeDocument")
    List<Object[]> getStatistiquesParType();

    @Query("SELECT FUNCTION('DATE', p.dateUpload), COUNT(p) FROM PieceJointe p " +
            "WHERE p.dateUpload BETWEEN :debut AND :fin " +
            "GROUP BY FUNCTION('DATE', p.dateUpload)")
    List<Object[]> countByDateUploadBetween(@Param("debut") LocalDateTime debut,
                                            @Param("fin") LocalDateTime fin);

    // ============================================================
    // Suppression
    // ============================================================

    void deleteByRecours(Recours recours);

    @Query("DELETE FROM PieceJointe p WHERE p.recours.id = :recoursId")
    void deleteByRecoursId(@Param("recoursId") Long recoursId);

    @Query("DELETE FROM PieceJointe p WHERE p.hashSha256 = :hash")
    void deleteByHash(@Param("hash") String hash);
}