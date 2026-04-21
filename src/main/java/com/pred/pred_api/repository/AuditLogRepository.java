package com.pred.pred_api.repository;

import com.pred.pred_api.model.AuditLog;
import com.pred.pred_api.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUtilisateurOrderByDateActionDesc(User utilisateur);

    List<AuditLog> findByActionOrderByDateActionDesc(String action);

    @Query("SELECT a FROM AuditLog a ORDER BY a.dateAction DESC")
    List<AuditLog> findAllByOrderByDateActionDesc();

    @Query("SELECT a FROM AuditLog a WHERE a.utilisateur.id = :userId ORDER BY a.dateAction DESC")
    List<AuditLog> findByUtilisateurId(@Param("userId") Long userId);

    List<AuditLog> findByDateActionBetween(LocalDateTime debut, LocalDateTime fin);

    @Query("SELECT a FROM AuditLog a WHERE a.adresseIp = :ip ORDER BY a.dateAction DESC")
    List<AuditLog> findByAdresseIp(@Param("ip") String ip);

    @Query("SELECT a.action, COUNT(a) FROM AuditLog a GROUP BY a.action")
    List<Object[]> countByAction();

    @Query("SELECT FUNCTION('DATE', a.dateAction), COUNT(a) FROM AuditLog a " +
            "WHERE a.dateAction BETWEEN :debut AND :fin " +
            "GROUP BY FUNCTION('DATE', a.dateAction)")
    List<Object[]> countByDateBetween(@Param("debut") LocalDateTime debut,
                                      @Param("fin") LocalDateTime fin);

    void deleteByDateActionBefore(LocalDateTime date);

    // Renommer la méthode pour éviter le conflit
    @Query("SELECT a FROM AuditLog a ORDER BY a.dateAction DESC")
    List<AuditLog> findRecentLogs(Pageable pageable);
}