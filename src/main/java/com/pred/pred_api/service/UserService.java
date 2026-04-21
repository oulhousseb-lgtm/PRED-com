package com.pred.pred_api.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.pred.pred_api.dto.*;
import com.pred.pred_api.exception.ResourceNotFoundException;
import com.pred.pred_api.model.User;
import com.pred.pred_api.model.enums.Role;
import com.pred.pred_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    // ============================================================
    // Enregistrement
    // ============================================================

    @Transactional
    public User register(RegisterRequest request) {
        // Validation des champs obligatoires
        validateRegisterRequest(request);

        // Vérification de l'unicité
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        if (request.getCin() != null && !request.getCin().isEmpty()) {
            if (userRepository.existsByCin(request.getCin())) {
                throw new RuntimeException("Ce CIN est déjà utilisé");
            }
        }

        // Vérification de la cohérence CIN/Majeur
        if (Boolean.TRUE.equals(request.getEstMajeur())) {
            if (request.getCin() == null || request.getCin().isEmpty()) {
                throw new RuntimeException("Le CIN est obligatoire pour les personnes majeures");
            }
        }

        // Création de l'utilisateur
        User user = User.builder()
                .email(request.getEmail())
                .motDePasse(passwordEncoder.encode(request.getMotDePasse()))
                .telephone(request.getTelephone())
                .cin(request.getCin())
                .nomFr(request.getNomFr())
                .prenomFr(request.getPrenomFr())
                .nomAr(request.getNomAr())
                .prenomAr(request.getPrenomAr())
                .genre(request.getGenre())
                .dateNaissance(request.getDateNaissance())
                .lieuNaissanceFr(request.getLieuNaissanceFr())
                .lieuNaissanceAr(request.getLieuNaissanceAr())
                .situationFamiliale(request.getSituationFamiliale())
                .professionFr(request.getProfessionFr())
                .professionAr(request.getProfessionAr())
                .adresseFr(request.getAdresseFr())
                .adresseAr(request.getAdresseAr())
                .estMajeur(request.getEstMajeur() != null ? request.getEstMajeur() : true)
                .role(Role.JUSTICIABLE)
                .actif(true)
                .build();

        User savedUser = userRepository.save(user);

        // Audit log
        auditLogService.logAction(savedUser, "REGISTER", "Inscription d'un nouveau justiciable");

        return savedUser;
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            throw new RuntimeException("L'email est obligatoire");
        }
        if (request.getMotDePasse() == null || request.getMotDePasse().length() < 6) {
            throw new RuntimeException("Le mot de passe doit contenir au moins 6 caractères");
        }
        if (request.getNomFr() == null || request.getNomFr().isEmpty()) {
            throw new RuntimeException("Le nom (français) est obligatoire");
        }
        if (request.getPrenomFr() == null || request.getPrenomFr().isEmpty()) {
            throw new RuntimeException("Le prénom (français) est obligatoire");
        }
        if (request.getNomAr() == null || request.getNomAr().isEmpty()) {
            throw new RuntimeException("الاسم العائلي (العربية) مطلوب");
        }
        if (request.getPrenomAr() == null || request.getPrenomAr().isEmpty()) {
            throw new RuntimeException("الاسم الشخصي (العربية) مطلوب");
        }
    }

    // ============================================================
    // Recherche
    // ============================================================

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'email : " + email));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID : " + id));
    }

    public List<User> findByRole(Role role) {
        return userRepository.findByRole(role);
    }

    public List<User> findAllActive() {
        return userRepository.findAll().stream()
                .filter(User::getActif)
                .collect(Collectors.toList());
    }

    // ============================================================
    // Gestion du profil
    // ============================================================

    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findById(userId);

        if (request.getTelephone() != null) {
            user.setTelephone(request.getTelephone());
        }
        if (request.getGenre() != null) {
            user.setGenre(request.getGenre());
        }
        if (request.getDateNaissance() != null) {
            user.setDateNaissance(request.getDateNaissance());
        }
        if (request.getLieuNaissanceFr() != null) {
            user.setLieuNaissanceFr(request.getLieuNaissanceFr());
        }
        if (request.getLieuNaissanceAr() != null) {
            user.setLieuNaissanceAr(request.getLieuNaissanceAr());
        }
        if (request.getSituationFamiliale() != null) {
            user.setSituationFamiliale(request.getSituationFamiliale());
        }
        if (request.getProfessionFr() != null) {
            user.setProfessionFr(request.getProfessionFr());
        }
        if (request.getProfessionAr() != null) {
            user.setProfessionAr(request.getProfessionAr());
        }
        if (request.getAdresseFr() != null) {
            user.setAdresseFr(request.getAdresseFr());
        }
        if (request.getAdresseAr() != null) {
            user.setAdresseAr(request.getAdresseAr());
        }

        User updatedUser = userRepository.save(user);
        auditLogService.logAction(user, "UPDATE_PROFILE", "Mise à jour du profil");

        return updatedUser;
    }

    @Transactional
    public void changePassword(User user, ChangePasswordRequest request) {
        // Validation
        if (!passwordEncoder.matches(request.getAncienMotDePasse(), user.getMotDePasse())) {
            throw new RuntimeException("Ancien mot de passe incorrect");
        }

        if (!request.getNouveauMotDePasse().equals(request.getConfirmationMotDePasse())) {
            throw new RuntimeException("Les nouveaux mots de passe ne correspondent pas");
        }

        if (request.getNouveauMotDePasse().length() < 6) {
            throw new RuntimeException("Le mot de passe doit contenir au moins 6 caractères");
        }

        user.setMotDePasse(passwordEncoder.encode(request.getNouveauMotDePasse()));
        userRepository.save(user);

        auditLogService.logAction(user, "CHANGE_PASSWORD", "Changement de mot de passe");
    }

    // ============================================================
    // Gestion du compte (Admin)
    // ============================================================

    @Transactional
    public User createUserByAdmin(RegisterRequest request, Role role) {
        User user = register(request);
        user.setRole(role);
        User savedUser = userRepository.save(user);

        auditLogService.logAction(null, "ADMIN_CREATE_USER",
                "Création d'un utilisateur avec le rôle : " + role);

        return savedUser;
    }

    @Transactional
    public void activateUser(Long userId) {
        User user = findById(userId);
        user.setActif(true);
        userRepository.save(user);

        auditLogService.logAction(null, "ACTIVATE_USER",
                "Activation du compte utilisateur ID : " + userId);
    }

    @Transactional
    public void deactivateUser(Long userId) {
        User user = findById(userId);
        user.setActif(false);
        userRepository.save(user);

        auditLogService.logAction(null, "DEACTIVATE_USER",
                "Désactivation du compte utilisateur ID : " + userId);
    }

    @Transactional
    public void changeUserRole(Long userId, Role newRole) {
        User user = findById(userId);
        Role oldRole = user.getRole();
        user.setRole(newRole);
        userRepository.save(user);

        auditLogService.logAction(null, "CHANGE_ROLE",
                "Changement de rôle de " + oldRole + " à " + newRole + " pour l'utilisateur ID : " + userId);
    }

    // ============================================================
    // Connexion
    // ============================================================

    @Transactional
    public void updateLastLogin(User user) {
        user.setDateDerniereConnexion(LocalDateTime.now());
        userRepository.save(user);
    }

    // ============================================================
    // Conversion DTO
    // ============================================================

    public com.pred.pred_api.dto.UserResponseDTO toDTO(User user) {
        return com.pred.pred_api.dto.UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .telephone(user.getTelephone())
                .cin(user.getCin())
                .nomFr(user.getNomFr())
                .prenomFr(user.getPrenomFr())
                .nomAr(user.getNomAr())
                .prenomAr(user.getPrenomAr())
                .genre(user.getGenre() != null ? user.getGenre().name() : null)
                .dateNaissance(user.getDateNaissance())
                .lieuNaissanceFr(user.getLieuNaissanceFr())
                .lieuNaissanceAr(user.getLieuNaissanceAr())
                .situationFamiliale(user.getSituationFamiliale() != null ? user.getSituationFamiliale().name() : null)
                .professionFr(user.getProfessionFr())
                .professionAr(user.getProfessionAr())
                .adresseFr(user.getAdresseFr())
                .adresseAr(user.getAdresseAr())
                .estMajeur(user.getEstMajeur())
                .role(user.getRole().name())
                .actif(user.getActif())
                .dateInscription(user.getDateInscription())
                .dateDerniereConnexion(user.getDateDerniereConnexion())
                .fullNameFr(user.getFullNameFr())
                .fullNameAr(user.getFullNameAr())
                .build();
    }

    // Ajouter ces méthodes dans UserService.java

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByCin(String cin) {
        return userRepository.existsByCin(cin);
    }

    public void deleteUser(Long id) {
        User user = findById(id);
        userRepository.delete(user);
        auditLogService.logAction(null, "DELETE_USER", "Suppression de l'utilisateur ID : " + id);
    }

    public List<UserResponseDTO> findAllDTO() {
        return userRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<UserResponseDTO> findRecentUsers(int limit) {
        Pageable pageable = PageRequest.of(0, limit);  // Maintenant PageRequest est importé
        return userRepository.findRecentUsers(pageable).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}