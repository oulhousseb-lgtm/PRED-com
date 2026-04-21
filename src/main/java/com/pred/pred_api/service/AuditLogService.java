package com.pred.pred_api.service;

import com.pred.pred_api.dto.AuditLogResponse;
import com.pred.pred_api.model.AuditLog;
import com.pred.pred_api.model.User;
import com.pred.pred_api.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    // ============================================================
    // Enregistrement des actions
    // ============================================================

    public void logAction(User utilisateur, String action, String description) {
        AuditLog auditLog = AuditLog.builder()
                .utilisateur(utilisateur)
                .action(action)
                .description(description)
                .adresseIp(getClientIp())
                .dateAction(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
    }

    private String getClientIp() {
        // À implémenter avec HttpServletRequest dans un contexte web
        return "SYSTEM";
    }

    // ============================================================
    // Consultation
    // ============================================================

    public List<AuditLog> getLogsUtilisateur(User utilisateur) {
        return auditLogRepository.findByUtilisateurOrderByDateActionDesc(utilisateur);
    }

    public List<AuditLog> getLogsByAction(String action) {
        return auditLogRepository.findByActionOrderByDateActionDesc(action);
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByDateActionDesc();
    }

    // Ajouter ces méthodes dans AuditLogService.java

    public List<AuditLogResponse> getAllLogsDTO() {
        return auditLogRepository.findAllByOrderByDateActionDesc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<AuditLogResponse> getLogsByUserDTO(Long userId) {
        return auditLogRepository.findByUtilisateurId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<AuditLogResponse> getLogsByActionDTO(String action) {
        return auditLogRepository.findByActionOrderByDateActionDesc(action).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Dans AuditLogService.java, remplacer la méthode getRecentLogs

    public List<AuditLogResponse> getRecentLogs(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return auditLogRepository.findRecentLogs(pageable).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private AuditLogResponse toDTO(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .utilisateurId(auditLog.getUtilisateur() != null ? auditLog.getUtilisateur().getId() : null)
                .utilisateurNom(auditLog.getUtilisateur() != null ? auditLog.getUtilisateur().getNomFr() : "SYSTEM")
                .utilisateurPrenom(auditLog.getUtilisateur() != null ? auditLog.getUtilisateur().getPrenomFr() : "")
                .utilisateurEmail(auditLog.getUtilisateur() != null ? auditLog.getUtilisateur().getEmail() : "")
                .action(auditLog.getAction())
                .description(auditLog.getDescription())
                .adresseIp(auditLog.getAdresseIp())
                .dateAction(auditLog.getDateAction())
                .build();
    }
}