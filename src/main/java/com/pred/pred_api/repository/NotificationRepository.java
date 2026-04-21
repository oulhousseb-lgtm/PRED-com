package com.pred.pred_api.repository;

import com.pred.pred_api.model.Notification;
import com.pred.pred_api.model.Recours;
import com.pred.pred_api.model.User;
import com.pred.pred_api.model.enums.TypeNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // ============================================================
    // Recherche par utilisateur
    // ============================================================

    List<Notification> findByUtilisateurOrderByDateEnvoiDesc(User utilisateur);

    List<Notification> findByUtilisateurAndLuFalse(User utilisateur);

    List<Notification> findByUtilisateurAndLuFalseOrderByDateEnvoiDesc(User utilisateur);

    @Query("SELECT n FROM Notification n WHERE n.utilisateur.id = :userId ORDER BY n.dateEnvoi DESC")
    List<Notification> findByUserId(@Param("userId") Long userId);

    long countByUtilisateurAndLuFalse(User utilisateur);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.utilisateur.id = :userId AND n.lu = false")
    long countUnreadByUserId(@Param("userId") Long userId);

    // ============================================================
    // Recherche par recours
    // ============================================================

    List<Notification> findByRecours(Recours recours);

    List<Notification> findByRecoursOrderByDateEnvoiDesc(Recours recours);

    @Query("SELECT n FROM Notification n WHERE n.recours.id = :recoursId ORDER BY n.dateEnvoi DESC")
    List<Notification> findByRecoursId(@Param("recoursId") Long recoursId);

    // ============================================================
    // Recherche par type
    // ============================================================

    List<Notification> findByTypeNotification(TypeNotification typeNotification);

    List<Notification> findByUtilisateurAndTypeNotification(User utilisateur, TypeNotification typeNotification);

    // ============================================================
    // Recherche par date
    // ============================================================

    List<Notification> findByDateEnvoiBetween(LocalDateTime debut, LocalDateTime fin);

    @Query("SELECT n FROM Notification n WHERE n.dateEnvoi > :date AND n.lu = false")
    List<Notification> findRecentUnread(@Param("date") LocalDateTime date);

    // ============================================================
    // Mise à jour
    // ============================================================

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.lu = true WHERE n.id = :id")
    void markAsRead(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.lu = true WHERE n.utilisateur.id = :userId")
    void markAllAsRead(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.lu = true WHERE n.utilisateur.id = :userId AND n.recours.id = :recoursId")
    void markAsReadByRecours(@Param("userId") Long userId, @Param("recoursId") Long recoursId);

    // ============================================================
    // Suppression
    // ============================================================

    void deleteByUtilisateur(User utilisateur);

    void deleteByRecours(Recours recours);

    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.lu = true AND n.dateEnvoi < :date")
    void deleteOldReadNotifications(@Param("date") LocalDateTime date);

    // ============================================================
    // Statistiques
    // ============================================================

    @Query("SELECT n.typeNotification, COUNT(n) FROM Notification n GROUP BY n.typeNotification")
    List<Object[]> countByType();

    @Query("SELECT FUNCTION('DATE', n.dateEnvoi), COUNT(n) FROM Notification n " +
            "WHERE n.dateEnvoi BETWEEN :debut AND :fin " +
            "GROUP BY FUNCTION('DATE', n.dateEnvoi)")
    List<Object[]> countByDateBetween(@Param("debut") LocalDateTime debut,
                                      @Param("fin") LocalDateTime fin);

    @Query("SELECT n.lu, COUNT(n) FROM Notification n GROUP BY n.lu")
    List<Object[]> countByReadStatus();
}