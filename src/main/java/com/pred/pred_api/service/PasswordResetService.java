package com.pred.pred_api.service;

import com.pred.pred_api.model.PasswordReset;
import com.pred.pred_api.model.User;
import com.pred.pred_api.repository.PasswordResetRepository;
import com.pred.pred_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetRepository passwordResetRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.password-reset.expiration-minutes:60}")
    private int expirationMinutes;

    @Transactional
    public void createPasswordResetToken(User user, String token) {
        // Désactiver les anciens tokens
        passwordResetRepository.deactivateOldTokens(user.getId());

        PasswordReset passwordReset = PasswordReset.builder()
                .utilisateur(user)
                .token(token)
                .dateExpiration(LocalDateTime.now().plusMinutes(expirationMinutes))
                .utilise(false)
                .build();

        passwordResetRepository.save(passwordReset);
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        return passwordResetRepository.findByTokenAndUtiliseFalse(token)
                .filter(pr -> pr.getDateExpiration().isAfter(LocalDateTime.now()))
                .map(pr -> {
                    User user = pr.getUtilisateur();
                    user.setMotDePasse(passwordEncoder.encode(newPassword));
                    userRepository.save(user);

                    pr.setUtilise(true);
                    passwordResetRepository.save(pr);

                    return true;
                })
                .orElse(false);
    }

    public boolean verifyToken(String token) {
        return passwordResetRepository.findByTokenAndUtiliseFalse(token)
                .map(pr -> pr.getDateExpiration().isAfter(LocalDateTime.now()))
                .orElse(false);
    }
}