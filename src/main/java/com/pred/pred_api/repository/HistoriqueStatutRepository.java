package com.pred.pred_api.repository;

import com.pred.pred_api.model.HistoriqueStatut;
import com.pred.pred_api.model.Recours;
import com.pred.pred_api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HistoriqueStatutRepository extends JpaRepository<HistoriqueStatut, Long> {

    // ============================================================
    // Recherche par recours
    // ============================================================

    List<HistoriqueStatut> findByRecoursOrderByDateModificationDesc(Recours recours);

    List<HistoriqueStatut> findByRecoursOrderByDateModificationAsc(Recours recours);

    @Query("SELECT h FROM HistoriqueStatut h WHERE h.recours.id = :recoursId ORDER BY h.dateModification DESC")
    List<HistoriqueStatut> findByRecoursId(@Param("recoursId") Long recoursId);

    @Query("SELECT h FROM HistoriqueStatut h WHERE h.recours = :recours AND h.nouveauStatut = :statut")
    List<HistoriqueStatut> findByRecoursAndNouveauStatut(@Param("recours") Recours recours,
                                                         @Param("statut") String statut);

    // ============================================================
    // Recherche par utilisateur (modifié par)
    // ============================================================

    List<HistoriqueStatut> findByModifieParOrderByDateModificationDesc(User modifiePar);

    @Query("SELECT h FROM HistoriqueStatut h WHERE h.modifiePar.id = :userId ORDER BY h.dateModification DESC")
    List<HistoriqueStatut> findByModifieParId(@Param("userId") Long userId);

    // ============================================================
    // Recherche par date
    // ============================================================

    List<HistoriqueStatut> findByDateModificationBetween(LocalDateTime debut, LocalDateTime fin);

    @Query("SELECT h FROM HistoriqueStatut h WHERE DATE(h.dateModification) = CURRENT_DATE")
    List<HistoriqueStatut> findModificationsDuJour();

    // ============================================================
    // Statistiques
    // ============================================================

    @Query("SELECT h.nouveauStatut, COUNT(h) FROM HistoriqueStatut h GROUP BY h.nouveauStatut")
    List<Object[]> countByNouveauStatut();

    @Query("SELECT FUNCTION('DATE', h.dateModification), COUNT(h) FROM HistoriqueStatut h " +
            "WHERE h.dateModification BETWEEN :debut AND :fin " +
            "GROUP BY FUNCTION('DATE', h.dateModification)")
    List<Object[]> countByDateBetween(@Param("debut") LocalDateTime debut,
                                      @Param("fin") LocalDateTime fin);

    @Query("SELECT AVG(TIMESTAMPDIFF(DAY, h1.dateModification, h2.dateModification)) " +
            "FROM HistoriqueStatut h1 JOIN HistoriqueStatut h2 ON h1.recours = h2.recours " +
            "WHERE h1.ancienStatut = 'DEPOSE' AND h2.nouveauStatut = 'JUGE'")
    Double getDelaiMoyenTraitement();

    // ============================================================
    // Suppression
    // ============================================================

    void deleteByRecours(Recours recours);

    @Query("DELETE FROM HistoriqueStatut h WHERE h.recours.id = :recoursId")
    void deleteByRecoursId(@Param("recoursId") Long recoursId);
}