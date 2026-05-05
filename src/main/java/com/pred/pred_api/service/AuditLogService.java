package com.pred.pred_api.service;

import com.pred.pred_api.dto.AuditLogResponse;
import com.pred.pred_api.model.AuditLog;
import com.pred.pred_api.model.User;
import com.pred.pred_api.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // ============================================================
    // DTO Methods - @Transactional pour éviter LazyInitializationException
    // ============================================================

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAllLogsDTO() {
        return auditLogRepository.findAllByOrderByDateActionDesc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getLogsByUserDTO(Long userId) {
        return auditLogRepository.findByUtilisateurId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getLogsByActionDTO(String action) {
        return auditLogRepository.findByActionOrderByDateActionDesc(action).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getRecentLogs(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return auditLogRepository.findRecentLogs(pageable).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private AuditLogResponse toDTO(AuditLog auditLog) {
        // Gestion sécurisée du User (évite LazyInitializationException)
        String nomUtilisateur = "SYSTEM";
        String prenomUtilisateur = "";
        String emailUtilisateur = "";
        Long utilisateurId = null;

        if (auditLog.getUtilisateur() != null) {
            try {
                nomUtilisateur = auditLog.getUtilisateur().getNomFr() != null ?
                        auditLog.getUtilisateur().getNomFr() : "SYSTEM";
                prenomUtilisateur = auditLog.getUtilisateur().getPrenomFr() != null ?
                        auditLog.getUtilisateur().getPrenomFr() : "";
                emailUtilisateur = auditLog.getUtilisateur().getEmail() != null ?
                        auditLog.getUtilisateur().getEmail() : "";
                utilisateurId = auditLog.getUtilisateur().getId();
            } catch (Exception e) {
                // En cas de LazyInitializationException, garder les valeurs par défaut
            }
        }

        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .utilisateurId(utilisateurId)
                .utilisateurNom(nomUtilisateur)
                .utilisateurPrenom(prenomUtilisateur)
                .utilisateurEmail(emailUtilisateur)
                .action(auditLog.getAction())
                .description(auditLog.getDescription())
                .adresseIp(auditLog.getAdresseIp())
                .dateAction(auditLog.getDateAction())
                .build();
    }
}