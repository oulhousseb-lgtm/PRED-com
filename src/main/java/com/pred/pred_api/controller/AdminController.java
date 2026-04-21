package com.pred.pred_api.controller;

import com.pred.pred_api.dto.*;
import com.pred.pred_api.model.User;
import com.pred.pred_api.model.enums.Role;
import com.pred.pred_api.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminController {

    private final UserService userService;
    private final TypeRecoursService typeRecoursService;
    private final RecoursService recoursService;
    private final AuditLogService auditLogService;
    private final StatisticsService statisticsService;

    // ============================================================
    // Gestion des utilisateurs
    // ============================================================

    @GetMapping("/users")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.findAllDTO());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(userService.toDTO(user));
    }

    @GetMapping("/users/role/{role}")
    public ResponseEntity<List<UserResponseDTO>> getUsersByRole(@PathVariable String role) {
        try {
            Role roleEnum = Role.valueOf(role.toUpperCase());
            List<User> users = userService.findByRole(roleEnum);
            List<UserResponseDTO> dtos = users.stream()
                    .map(userService::toDTO)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponseDTO> createUser(
            @Valid @RequestBody RegisterRequest request,
            @RequestParam Role role) {
        User user = userService.createUserByAdmin(request, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.toDTO(user));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequest request) {
        User user = userService.updateProfile(id, request);
        return ResponseEntity.ok(userService.toDTO(user));
    }

    @PutMapping("/users/{id}/activate")
    public ResponseEntity<Map<String, String>> activateUser(@PathVariable Long id) {
        userService.activateUser(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Utilisateur activé avec succès");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{id}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Utilisateur désactivé avec succès");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<Map<String, String>> changeUserRole(
            @PathVariable Long id,
            @RequestParam Role newRole) {
        userService.changeUserRole(id, newRole);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Rôle modifié avec succès");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Utilisateur supprimé avec succès");
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // Gestion des types de recours
    // ============================================================

    @GetMapping("/types-recours")
    public ResponseEntity<List<TypeRecoursResponse>> getAllTypesRecours() {
        return ResponseEntity.ok(typeRecoursService.findAll());
    }

    @GetMapping("/types-recours/actifs")
    public ResponseEntity<List<TypeRecoursResponse>> getActiveTypesRecours() {
        return ResponseEntity.ok(typeRecoursService.findAllActive());
    }

    @GetMapping("/types-recours/{id}")
    public ResponseEntity<TypeRecoursResponse> getTypeRecoursById(@PathVariable Long id) {
        return ResponseEntity.ok(typeRecoursService.findById(id));
    }

    @GetMapping("/types-recours/categorie/{categorie}")
    public ResponseEntity<List<TypeRecoursResponse>> getTypesRecoursByCategorie(
            @PathVariable String categorie) {
        return ResponseEntity.ok(typeRecoursService.findByCategorie(categorie));
    }

    @PostMapping("/types-recours")
    public ResponseEntity<TypeRecoursResponse> createTypeRecours(
            @Valid @RequestBody TypeRecoursRequest request) {
        TypeRecoursResponse response = typeRecoursService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/types-recours/{id}")
    public ResponseEntity<TypeRecoursResponse> updateTypeRecours(
            @PathVariable Long id,
            @Valid @RequestBody TypeRecoursRequest request) {
        return ResponseEntity.ok(typeRecoursService.update(id, request));
    }

    @PutMapping("/types-recours/{id}/activate")
    public ResponseEntity<Map<String, String>> activateTypeRecours(@PathVariable Long id) {
        typeRecoursService.activate(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Type de recours activé avec succès");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/types-recours/{id}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateTypeRecours(@PathVariable Long id) {
        typeRecoursService.deactivate(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Type de recours désactivé avec succès");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/types-recours/{id}")
    public ResponseEntity<Map<String, String>> deleteTypeRecours(@PathVariable Long id) {
        typeRecoursService.delete(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Type de recours supprimé avec succès");
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // Statistiques globales
    // ============================================================

    @GetMapping("/statistiques")
    public ResponseEntity<StatistiquesResponse> getStatistiquesGlobales() {
        return ResponseEntity.ok(statisticsService.getStatistiquesGlobales());
    }

    @GetMapping("/statistiques/period")
    public ResponseEntity<StatistiquesResponse> getStatistiquesByPeriod(
            @RequestParam String debut,
            @RequestParam String fin) {
        return ResponseEntity.ok(statisticsService.getStatistiquesByPeriod(debut, fin));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("stats", statisticsService.getStatistiquesGlobales());
        dashboard.put("recoursRecents", recoursService.findRecentRecours(10));
        dashboard.put("utilisateursRecents", userService.findRecentUsers(10));
        dashboard.put("activiteRecente", auditLogService.getRecentLogs(20));
        return ResponseEntity.ok(dashboard);
    }

    // ============================================================
    // Audit Logs
    // ============================================================

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLogResponse>> getAllAuditLogs() {
        return ResponseEntity.ok(auditLogService.getAllLogsDTO());
    }

    @GetMapping("/audit-logs/user/{userId}")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(auditLogService.getLogsByUserDTO(userId));
    }

    @GetMapping("/audit-logs/action/{action}")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogsByAction(@PathVariable String action) {
        return ResponseEntity.ok(auditLogService.getLogsByActionDTO(action));
    }

    // ============================================================
    // Configuration système
    // ============================================================

    @GetMapping("/configurations")
    public ResponseEntity<Map<String, String>> getConfigurations() {
        return ResponseEntity.ok(statisticsService.getConfigurations());
    }

    @PutMapping("/configurations/{cle}")
    public ResponseEntity<Map<String, String>> updateConfiguration(
            @PathVariable String cle,
            @RequestBody Map<String, String> request) {
        statisticsService.updateConfiguration(cle, request.get("valeur"));
        Map<String, String> response = new HashMap<>();
        response.put("message", "Configuration mise à jour avec succès");
        return ResponseEntity.ok(response);
    }
}