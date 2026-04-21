package com.pred.pred_api.repository;

import com.pred.pred_api.model.Session;
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
public interface SessionRepository extends JpaRepository<Session, Long> {

    // ============================================================
    // Recherche de sessions
    // ============================================================

    Optional<Session> findByTokenJwt(String tokenJwt);

    List<Session> findByUtilisateur(User utilisateur);

    List<Session> findByUtilisateurAndActifTrue(User utilisateur);

    @Query("SELECT s FROM Session s WHERE s.utilisateur.id = :userId AND s.actif = true")
    List<Session> findActiveSessionsByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM Session s WHERE s.adresseIp = :ip AND s.actif = true")
    List<Session> findActiveSessionsByIp(@Param("ip") String ip);

    // ============================================================
    // Vérification
    // ============================================================

    boolean existsByTokenJwtAndActifTrue(String tokenJwt);

    @Query("SELECT COUNT(s) > 0 FROM Session s WHERE s.tokenJwt = :token AND s.actif = true AND s.dateExpiration > :now")
    boolean isSessionValid(@Param("token") String token, @Param("now") LocalDateTime now);

    // ============================================================
    // Gestion des sessions
    // ============================================================

    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.actif = false WHERE s.utilisateur.id = :userId")
    void deactivateAllUserSessions(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.actif = false WHERE s.tokenJwt = :token")
    void deactivateByToken(@Param("token") String token);

    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.actif = false WHERE s.dateExpiration < :now")
    void deactivateExpiredSessions(@Param("now") LocalDateTime now);

    // ============================================================
    // Suppression
    // ============================================================

    void deleteByUtilisateur(User utilisateur);

    void deleteByTokenJwt(String tokenJwt);

    @Modifying
    @Transactional
    @Query("DELETE FROM Session s WHERE s.actif = false AND s.dateExpiration < :date")
    void deleteInactiveExpiredSessions(@Param("date") LocalDateTime date);

    // ============================================================
    // Statistiques
    // ============================================================

    long countByActifTrue();

    @Query("SELECT COUNT(DISTINCT s.utilisateur.id) FROM Session s WHERE s.actif = true")
    long countActiveUsers();

    @Query("SELECT s.adresseIp, COUNT(s) FROM Session s WHERE s.actif = true GROUP BY s.adresseIp")
    List<Object[]> countActiveSessionsByIp();
}