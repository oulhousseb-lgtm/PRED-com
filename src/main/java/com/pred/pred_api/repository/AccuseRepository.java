package com.pred.pred_api.repository;

import com.pred.pred_api.model.Accuse;
import com.pred.pred_api.model.Recours;
import com.pred.pred_api.model.enums.SituationPenale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccuseRepository extends JpaRepository<Accuse, Long> {

    // ============================================================
    // Recherche par recours
    // ============================================================

    List<Accuse> findByRecours(Recours recours);

    @Query("SELECT a FROM Accuse a WHERE a.recours.id = :recoursId")
    List<Accuse> findByRecoursId(@Param("recoursId") Long recoursId);

    long countByRecours(Recours recours);

    // ============================================================
    // Recherche par CIN
    // ============================================================

    Optional<Accuse> findByCin(String cin);

    List<Accuse> findByCinContaining(String cin);

    // ============================================================
    // Recherche par situation pénale
    // ============================================================

    List<Accuse> findBySituationPenale(SituationPenale situationPenale);

    List<Accuse> findByRecoursAndSituationPenale(Recours recours, SituationPenale situationPenale);

    List<Accuse> findBySituationPenaleAndLieuDetentionContaining(SituationPenale situationPenale, String lieu);

    // ============================================================
    // Recherche par nom
    // ============================================================

    @Query("SELECT a FROM Accuse a WHERE " +
            "LOWER(a.nomFr) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.prenomFr) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.nomAr) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.prenomAr) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Accuse> searchByKeyword(@Param("keyword") String keyword);

    // ============================================================
    // Statistiques
    // ============================================================

    @Query("SELECT a.situationPenale, COUNT(a) FROM Accuse a GROUP BY a.situationPenale")
    List<Object[]> countBySituationPenale();

    @Query("SELECT a.estMajeur, COUNT(a) FROM Accuse a GROUP BY a.estMajeur")
    List<Object[]> countByEstMajeur();

    // ============================================================
    // Suppression
    // ============================================================

    void deleteByRecours(Recours recours);

    @Query("DELETE FROM Accuse a WHERE a.recours.id = :recoursId")
    void deleteByRecoursId(@Param("recoursId") Long recoursId);
}