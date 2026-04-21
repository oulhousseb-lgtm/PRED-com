package com.pred.pred_api.controller;

import com.pred.pred_api.dto.*;
import com.pred.pred_api.model.User;
import com.pred.pred_api.model.enums.Role;
import com.pred.pred_api.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;
    private final AuditLogService auditLogService;

    // ============================================================
    // Authentification
    // ============================================================

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getMotDePasse())
        );

        User user = userService.findByEmail(request.getEmail());
        userService.updateLastLogin(user);

        String token = jwtService.generateToken(user.getEmail());

        auditLogService.logAction(user, "LOGIN", "Connexion réussie");

        return ResponseEntity.ok(LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .nomFr(user.getNomFr())
                .prenomFr(user.getPrenomFr())
                .nomAr(user.getNomAr())
                .prenomAr(user.getPrenomAr())
                .build());
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return ResponseEntity.ok(userService.toDTO(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            User user = userService.findByEmail(userDetails.getUsername());
            auditLogService.logAction(user, "LOGOUT", "Déconnexion");
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Déconnexion réussie");
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // Mot de passe oublié
    // ============================================================

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            User user = userService.findByEmail(request.getEmail());
            String token = UUID.randomUUID().toString();
            passwordResetService.createPasswordResetToken(user, token);

            // TODO: Envoyer l'email avec le token

            Map<String, String> response = new HashMap<>();
            response.put("message", "Un email de réinitialisation a été envoyé");
            response.put("status", "SUCCESS");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Ne pas révéler si l'email existe ou non (sécurité)
            Map<String, String> response = new HashMap<>();
            response.put("message", "Si cet email existe, un lien de réinitialisation a été envoyé");
            response.put("status", "SUCCESS");
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        if (!request.getNouveauMotDePasse().equals(request.getConfirmationMotDePasse())) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Les mots de passe ne correspondent pas");
            return ResponseEntity.badRequest().body(response);
        }

        boolean success = passwordResetService.resetPassword(request.getToken(), request.getNouveauMotDePasse());

        Map<String, String> response = new HashMap<>();
        if (success) {
            response.put("message", "Mot de passe réinitialisé avec succès");
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Token invalide ou expiré");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/verify-reset-token")
    public ResponseEntity<Map<String, Object>> verifyResetToken(@RequestParam String token) {
        boolean valid = passwordResetService.verifyToken(token);

        Map<String, Object> response = new HashMap<>();
        response.put("valid", valid);
        response.put("message", valid ? "Token valide" : "Token invalide ou expiré");
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // Gestion du profil
    // ============================================================

    @GetMapping("/profile")
    public ResponseEntity<UserResponseDTO> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(userService.toDTO(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponseDTO> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        User user = userService.findByEmail(userDetails.getUsername());
        User updatedUser = userService.updateProfile(user.getId(), request);
        auditLogService.logAction(user, "UPDATE_PROFILE", "Mise à jour du profil");
        return ResponseEntity.ok(userService.toDTO(updatedUser));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        User user = userService.findByEmail(userDetails.getUsername());
        userService.changePassword(user, request);
        auditLogService.logAction(user, "CHANGE_PASSWORD", "Changement de mot de passe");

        Map<String, String> response = new HashMap<>();
        response.put("message", "Mot de passe modifié avec succès");
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // Validation
    // ============================================================

    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam String email) {
        boolean exists = userService.existsByEmail(email);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-cin")
    public ResponseEntity<Map<String, Boolean>> checkCin(@RequestParam String cin) {
        boolean exists = userService.existsByCin(cin);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }
}