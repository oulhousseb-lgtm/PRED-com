package com.pred.pred_api.repository;

import com.pred.pred_api.model.DemandePiece;
import com.pred.pred_api.model.Recours;
import com.pred.pred_api.model.User;
import com.pred.pred_api.model.enums.StatutDemandePiece;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DemandePieceRepository extends JpaRepository<DemandePiece, Long> {

    // ============================================================
    // Recherche par statut
    // ============================================================
    List<DemandePiece> findByStatut(StatutDemandePiece statut);
    long countByStatut(StatutDemandePiece statut);

    @Query("SELECT d FROM DemandePiece d WHERE d.statut = :statut ORDER BY d.dateEnvoi DESC")
    List<DemandePiece> findByStatutOrderByDateEnvoiDesc(@Param("statut") StatutDemandePiece statut);

    // ============================================================
    // Recherche par recours
    // ============================================================
    List<DemandePiece> findByRecours(Recours recours);
    List<DemandePiece> findByRecoursOrderByDateEnvoiDesc(Recours recours);

    @Query("SELECT d FROM DemandePiece d WHERE d.recours.id = :recoursId")
    List<DemandePiece> findByRecoursId(@Param("recoursId") Long recoursId);

    @Query("SELECT COUNT(d) FROM DemandePiece d WHERE d.recours = :recours AND d.statut = :statut")
    long countByRecoursAndStatut(@Param("recours") Recours recours,
                                 @Param("statut") StatutDemandePiece statut);

    // ============================================================
    // Recherche par greffier
    // ============================================================
    List<DemandePiece> findByGreffier(User greffier);
    List<DemandePiece> findByGreffierOrderByDateEnvoiDesc(User greffier);

    @Query("SELECT d FROM DemandePiece d WHERE d.greffier.id = :greffierId")
    List<DemandePiece> findByGreffierId(@Param("greffierId") Long greffierId);

    // ============================================================
    // Recherche par date d'expiration
    // ============================================================
    @Query("SELECT d FROM DemandePiece d WHERE d.dateExpiration < :date AND d.statut = 'EN_ATTENTE'")
    List<DemandePiece> findExpirees(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(d) FROM DemandePiece d WHERE d.dateExpiration < :date AND d.statut = 'EN_ATTENTE'")
    long countExpirees(@Param("date") LocalDateTime date);

    @Query("SELECT d FROM DemandePiece d WHERE d.dateExpiration BETWEEN :debut AND :fin")
    List<DemandePiece> findByDateExpirationBetween(@Param("debut") LocalDateTime debut,
                                                   @Param("fin") LocalDateTime fin);

    List<DemandePiece> findByStatutAndDateExpirationBefore(StatutDemandePiece statut, LocalDateTime date);
    List<DemandePiece> findByStatutAndDateExpirationAfter(StatutDemandePiece statut, LocalDateTime date);

    // ============================================================
    // Recherche urgente
    // ============================================================
    @Query("SELECT d FROM DemandePiece d WHERE d.statut = 'EN_ATTENTE' AND d.dateExpiration > :now AND d.dateExpiration < :urgence ORDER BY d.dateExpiration ASC")
    List<DemandePiece> findUrgentes(@Param("now") LocalDateTime now,
                                    @Param("urgence") LocalDateTime urgence);

    // ============================================================
    // Statistiques
    // ============================================================
    @Query("SELECT d.statut, COUNT(d) FROM DemandePiece d GROUP BY d.statut")
    List<Object[]> countGroupByStatut();

    @Query("SELECT AVG(d.delaiJour) FROM DemandePiece d")
    Double getDelaiMoyen();

    @Query("SELECT d.greffier.id, COUNT(d) FROM DemandePiece d GROUP BY d.greffier.id")
    List<Object[]> countByGreffier();

    @Query("SELECT FUNCTION('DATE', d.dateEnvoi), COUNT(d) FROM DemandePiece d GROUP BY FUNCTION('DATE', d.dateEnvoi)")
    List<Object[]> countByDateEnvoi();

    // ============================================================
    // Suppression
    // ============================================================
    void deleteByRecours(Recours recours);

    @Query("DELETE FROM DemandePiece d WHERE d.recours.id = :recoursId")
    void deleteByRecoursId(@Param("recoursId") Long recoursId);

    @Query("DELETE FROM DemandePiece d WHERE d.statut = 'EXPIREE' AND d.dateExpiration < :date")
    void deleteOldExpirees(@Param("date") LocalDateTime date);
    void deleteById(Long id);
}