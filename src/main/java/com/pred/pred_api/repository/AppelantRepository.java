package com.pred.pred_api.repository;

import com.pred.pred_api.model.Appelant;
import com.pred.pred_api.model.Recours;
import com.pred.pred_api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppelantRepository extends JpaRepository<Appelant, Long> {

    // ============================================================
    // Recherche par recours
    // ============================================================

    List<Appelant> findByRecours(Recours recours);

    @Query("SELECT a FROM Appelant a WHERE a.recours.id = :recoursId")
    List<Appelant> findByRecoursId(@Param("recoursId") Long recoursId);

    long countByRecours(Recours recours);

    // ============================================================
    // Recherche par utilisateur
    // ============================================================

    List<Appelant> findByUtilisateur(User utilisateur);

    Optional<Appelant> findByRecoursAndUtilisateur(Recours recours, User utilisateur);

    @Query("SELECT a FROM Appelant a WHERE a.utilisateur.id = :userId")
    List<Appelant> findByUtilisateurId(@Param("userId") Long userId);

    // ============================================================
    // Recherche par CIN
    // ============================================================

    Optional<Appelant> findByCin(String cin);

    List<Appelant> findByCinContaining(String cin);

    // ============================================================
    // Suppression
    // ============================================================

    void deleteByRecours(Recours recours);

    @Query("DELETE FROM Appelant a WHERE a.recours.id = :recoursId")
    void deleteByRecoursId(@Param("recoursId") Long recoursId);
}