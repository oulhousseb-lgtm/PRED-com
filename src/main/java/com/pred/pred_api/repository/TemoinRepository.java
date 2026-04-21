package com.pred.pred_api.repository;

import com.pred.pred_api.model.Temoin;
import com.pred.pred_api.model.Recours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemoinRepository extends JpaRepository<Temoin, Long> {

    // ============================================================
    // Recherche par recours
    // ============================================================

    List<Temoin> findByRecours(Recours recours);

    @Query("SELECT t FROM Temoin t WHERE t.recours.id = :recoursId")
    List<Temoin> findByRecoursId(@Param("recoursId") Long recoursId);

    long countByRecours(Recours recours);

    // ============================================================
    // Recherche par CIN
    // ============================================================

    Optional<Temoin> findByCin(String cin);

    List<Temoin> findByCinContaining(String cin);

    // ============================================================
    // Recherche par téléphone
    // ============================================================

    Optional<Temoin> findByTelephone(String telephone);

    // ============================================================
    // Recherche par nom
    // ============================================================

    @Query("SELECT t FROM Temoin t WHERE " +
            "LOWER(t.nomFr) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.prenomFr) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.nomAr) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.prenomAr) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Temoin> searchByKeyword(@Param("keyword") String keyword);

    // ============================================================
    // Suppression
    // ============================================================

    void deleteByRecours(Recours recours);

    @Query("DELETE FROM Temoin t WHERE t.recours.id = :recoursId")
    void deleteByRecoursId(@Param("recoursId") Long recoursId);
}