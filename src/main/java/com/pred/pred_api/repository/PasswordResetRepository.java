package com.pred.pred_api.repository;

import com.pred.pred_api.model.PasswordReset;
import com.pred.pred_api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

    // ============================================================
    // Recherche de tokens
    // ============================================================

    Optional<PasswordReset> findByToken(String token);

    Optional<PasswordReset> findByTokenAndUtiliseFalse(String token);

    List<PasswordReset> findByUtilisateur(User utilisateur);

    List<PasswordReset> findByUtilisateurOrderByDateCreationDesc(User utilisateur);

    @Query("SELECT p FROM PasswordReset p WHERE p.utilisateur.id = :userId AND p.utilise = false")
    List<PasswordReset> findActiveTokensByUserId(@Param("userId") Long userId);

    // ============================================================
    // Vérification
    // ============================================================

    boolean existsByTokenAndUtiliseFalse(String token);

    @Query("SELECT COUNT(p) > 0 FROM PasswordReset p WHERE p.token = :token AND p.utilise = false AND p.dateExpiration > :now")
    boolean isTokenValid(@Param("token") String token, @Param("now") LocalDateTime now);

    // ============================================================
    // Suppression/nettoyage
    // ============================================================

    void deleteByUtilisateur(User utilisateur);

    @Modifying
    @Transactional
    @Query("UPDATE PasswordReset p SET p.utilise = true WHERE p.utilisateur.id = :userId AND p.utilise = false")
    void deactivateOldTokens(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordReset p WHERE p.dateExpiration < :date")
    void deleteExpiredTokens(@Param("date") LocalDateTime date);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordReset p WHERE p.utilise = true AND p.dateCreation < :date")
    void deleteOldUsedTokens(@Param("date") LocalDateTime date);

    // ============================================================
    // Statistiques
    // ============================================================

    long countByUtiliseFalse();

    long countByDateExpirationBeforeAndUtiliseFalse(LocalDateTime date);

    @Query("SELECT COUNT(p) FROM PasswordReset p WHERE p.dateCreation BETWEEN :debut AND :fin")
    long countByPeriod(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);
}