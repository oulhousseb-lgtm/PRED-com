package com.pred.pred_api.service;

import com.pred.pred_api.dto.UserResponseDTO;
import com.pred.pred_api.model.User;
import com.pred.pred_api.model.enums.Role;
import com.pred.pred_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return toDTO(user);
    }

    public List<UserResponseDTO> getUsersByRole(Role role) {
        return userRepository.findByRole(role).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public void activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        user.setActif(true);
        userRepository.save(user);
        auditLogService.logAction(null, "ACTIVATE_USER", "Utilisateur activé : " + user.getEmail());
    }

    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        user.setActif(false);
        userRepository.save(user);
        auditLogService.logAction(null, "DEACTIVATE_USER", "Utilisateur désactivé : " + user.getEmail());
    }

    public void changeUserRole(Long id, Role newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        Role oldRole = user.getRole();
        user.setRole(newRole);
        userRepository.save(user);
        auditLogService.logAction(null, "CHANGE_ROLE",
                "Rôle changé de " + oldRole + " à " + newRole + " pour : " + user.getEmail());
    }

    private UserResponseDTO toDTO(User user) {
        return UserResponseDTO.builder()
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
                .fullNameFr(user.getPrenomFr() + " " + user.getNomFr())  // Correction ici
                .fullNameAr(user.getPrenomAr() + " " + user.getNomAr())  // Correction ici
                .build();
    }
}