package com.pred.pred_api.service;

import com.pred.pred_api.dto.DemandePieceRequest;
import com.pred.pred_api.dto.DemandePieceResponse;
import com.pred.pred_api.exception.ResourceNotFoundException;
import com.pred.pred_api.model.DemandePiece;
import com.pred.pred_api.model.Recours;
import com.pred.pred_api.model.User;
import com.pred.pred_api.model.enums.StatutDemandePiece;
import com.pred.pred_api.model.enums.TypeNotification;
import com.pred.pred_api.repository.DemandePieceRepository;
import com.pred.pred_api.repository.RecoursRepository;
import com.pred.pred_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DemandePieceService {

    private final DemandePieceRepository demandePieceRepository;
    private final RecoursRepository recoursRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public DemandePieceResponse createDemande(Long recoursId, Long greffierId, DemandePieceRequest request) {
        Recours recours = recoursRepository.findById(recoursId)
                .orElseThrow(() -> new ResourceNotFoundException("Recours non trouvé"));

        User greffier = userRepository.findById(greffierId)
                .orElseThrow(() -> new ResourceNotFoundException("Greffier non trouvé"));

        // تخزين الرسالة باللغتين في حقل واحد مفصول بـ ||
        String combinedMessage = request.getMessageFr() + "||" + (request.getMessageAr() != null ? request.getMessageAr() : request.getMessageFr());

        DemandePiece demande = DemandePiece.builder()
                .recours(recours)
                .greffier(greffier)
                .message(combinedMessage)
                .delaiJour(request.getDelaiJour())
                .dateEnvoi(LocalDateTime.now())
                .dateExpiration(LocalDateTime.now().plusDays(request.getDelaiJour()))
                .statut(StatutDemandePiece.EN_ATTENTE)
                .build();

        DemandePiece saved = demandePieceRepository.save(demande);

        notificationService.notifierUtilisateur(recours.getUtilisateur(), recours,
                "Demande de pièces complémentaires",
                request.getMessageFr(),
                "طلب وثائق إضافية",
                request.getMessageAr(),
                TypeNotification.EMAIL);

        return toResponse(saved);
    }

    public List<DemandePieceResponse> findByRecours(Long recoursId) {
        Recours recours = recoursRepository.findById(recoursId)
                .orElseThrow(() -> new ResourceNotFoundException("Recours non trouvé"));
        List<DemandePiece> demandes = demandePieceRepository.findByRecours(recours);
        if (demandes == null || demandes.isEmpty()) {
            return new ArrayList<>();
        }
        return demandes.stream()
                .map(this::toResponse)
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }

    public List<DemandePieceResponse> findAll() {
        try {
            List<DemandePiece> demandes = demandePieceRepository.findAll();
            System.out.println("=== DEBUG findAll ===");
            System.out.println("Nombre de demandes trouvées en DB: " + (demandes != null ? demandes.size() : 0));

            if (demandes == null || demandes.isEmpty()) {
                return new ArrayList<>();
            }

            List<DemandePieceResponse> responses = new ArrayList<>();
            for (DemandePiece demande : demandes) {
                try {
                    DemandePieceResponse response = toResponse(demande);
                    if (response != null) {
                        responses.add(response);
                        System.out.println("✓ Demande ID " + demande.getId() + " convertie avec succès");
                    } else {
                        System.err.println("✗ Demande ID " + demande.getId() + " retourne null");
                    }
                } catch (Exception e) {
                    System.err.println("✗ Erreur conversion demande ID " + demande.getId() + ": " + e.getMessage());
                }
            }
            System.out.println("Total converties: " + responses.size());
            return responses;
        } catch (Exception e) {
            System.err.println("Erreur findAll: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<DemandePieceResponse> findEnAttente() {
        try {
            List<DemandePiece> demandes = demandePieceRepository.findByStatut(StatutDemandePiece.EN_ATTENTE);
            if (demandes == null || demandes.isEmpty()) {
                return new ArrayList<>();
            }
            return demandes.stream()
                    .map(this::toResponse)
                    .filter(r -> r != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Erreur findEnAttente: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<DemandePieceResponse> findExpirees() {
        try {
            List<DemandePiece> demandes = demandePieceRepository.findExpirees(LocalDateTime.now());
            if (demandes == null || demandes.isEmpty()) {
                return new ArrayList<>();
            }
            return demandes.stream()
                    .map(this::toResponse)
                    .filter(r -> r != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Erreur findExpirees: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<DemandePieceResponse> findUrgentes() {
        try {
            LocalDateTime dans3Jours = LocalDateTime.now().plusDays(3);
            List<DemandePiece> demandes = demandePieceRepository.findByStatutAndDateExpirationBefore(
                    StatutDemandePiece.EN_ATTENTE, dans3Jours);
            if (demandes == null || demandes.isEmpty()) {
                return new ArrayList<>();
            }
            return demandes.stream()
                    .map(this::toResponse)
                    .filter(r -> r != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Erreur findUrgentes: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Transactional
    public void marquerSatisfaite(Long id) {
        DemandePiece demande = demandePieceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande non trouvée"));
        demande.setStatut(StatutDemandePiece.SATISFAITE);
        demandePieceRepository.save(demande);
    }

    public long countEnAttente() {
        try {
            return demandePieceRepository.countByStatut(StatutDemandePiece.EN_ATTENTE);
        } catch (Exception e) {
            return 0;
        }
    }

    public long countExpirees() {
        try {
            return demandePieceRepository.countExpirees(LocalDateTime.now());
        } catch (Exception e) {
            return 0;
        }
    }

    public long countSatisfaites() {
        try {
            return demandePieceRepository.countByStatut(StatutDemandePiece.SATISFAITE);
        } catch (Exception e) {
            return 0;
        }
    }

    // ============================================================
    // Méthodes de récupération directe avec logs
    // ============================================================

    public List<DemandePieceResponse> getAllDemandesDirect() {
        try {
            List<DemandePiece> demandes = demandePieceRepository.findAll();
            System.out.println("=== getAllDemandesDirect ===");
            System.out.println("Demandes en DB: " + (demandes != null ? demandes.size() : 0));

            if (demandes == null || demandes.isEmpty()) {
                return new ArrayList<>();
            }

            List<DemandePieceResponse> responses = new ArrayList<>();
            for (DemandePiece demande : demandes) {
                try {
                    DemandePieceResponse response = toResponse(demande);
                    if (response != null) {
                        responses.add(response);
                    } else {
                        System.err.println("Response null pour ID: " + demande.getId());
                    }
                } catch (Exception e) {
                    System.err.println("Erreur pour ID " + demande.getId() + ": " + e.getMessage());
                }
            }
            System.out.println("Responses générées: " + responses.size());
            return responses;
        } catch (Exception e) {
            System.err.println("Erreur getAllDemandesDirect: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<DemandePieceResponse> getDemandesByRecoursId(Long recoursId) {
        try {
            List<DemandePiece> demandes = demandePieceRepository.findByRecoursId(recoursId);
            if (demandes == null || demandes.isEmpty()) {
                return new ArrayList<>();
            }
            return demandes.stream()
                    .map(this::toResponse)
                    .filter(r -> r != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Erreur getDemandesByRecoursId: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ============================================================
    // Méthode toResponse améliorée avec gestion d'erreurs
    // ============================================================
    @Transactional(readOnly = true)
    private DemandePieceResponse toResponse(DemandePiece demande) {
        try {
            if (demande == null) {
                System.err.println("toResponse: demande est null");
                return null;
            }

            // Calcul des jours restants
            long joursRestants = 0;
            try {
                if (demande.getDateExpiration() != null) {
                    joursRestants = ChronoUnit.DAYS.between(LocalDateTime.now(), demande.getDateExpiration());
                }
            } catch (Exception e) {
                joursRestants = 0;
            }

            // Extraction des messages
            String messageFr = "";
            String messageAr = "";
            String message = demande.getMessage();
            if (message != null && message.contains("||")) {
                String[] parts = message.split("\\|\\|");
                messageFr = parts.length > 0 ? parts[0] : message;
                messageAr = parts.length > 1 ? parts[1] : message;
            } else {
                messageFr = message != null ? message : "";
                messageAr = message != null ? message : "";
            }

            // Récupération des infos du recours
            Long recoursId = null;
            String recoursNumero = "N/A";
            try {
                if (demande.getRecours() != null) {
                    recoursId = demande.getRecours().getId();
                    recoursNumero = demande.getRecours().getNumeroRecours();
                    if (recoursNumero == null) recoursNumero = "Recours #" + recoursId;
                }
            } catch (Exception e) {
                recoursNumero = "Recours inconnu";
            }

            // Récupération des infos du greffier
            Long greffierId = null;
            String greffierNom = "";
            String greffierPrenom = "";
            try {
                if (demande.getGreffier() != null) {
                    greffierId = demande.getGreffier().getId();
                    greffierNom = demande.getGreffier().getNomFr();
                    if (greffierNom == null) greffierNom = "";
                    greffierPrenom = demande.getGreffier().getPrenomFr();
                    if (greffierPrenom == null) greffierPrenom = "";
                }
            } catch (Exception e) {
                // Ignorer
            }

            // Déterminer si expirée
            boolean estExpiree = false;
            try {
                if (demande.getDateExpiration() != null) {
                    estExpiree = demande.getDateExpiration().isBefore(LocalDateTime.now());
                }
            } catch (Exception e) {
                estExpiree = false;
            }

            return DemandePieceResponse.builder()
                    .id(demande.getId())
                    .recoursId(recoursId)
                    .recoursNumero(recoursNumero)
                    .greffierId(greffierId)
                    .greffierNom(greffierNom)
                    .greffierPrenom(greffierPrenom)
                    .messageFr(messageFr)
                    .messageAr(messageAr)
                    .delaiJour(demande.getDelaiJour())
                    .dateEnvoi(demande.getDateEnvoi())
                    .dateExpiration(demande.getDateExpiration())
                    .statut(demande.getStatut() != null ? demande.getStatut().name() : "EN_ATTENTE")
                    .estExpiree(estExpiree)
                    .joursRestants(joursRestants > 0 ? joursRestants : 0)
                    .build();

        } catch (Exception e) {
            System.err.println("Erreur dans toResponse pour demande ID " + (demande != null ? demande.getId() : "null") + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Transactional
    public void deleteDemande(Long id) {
        DemandePiece demande = demandePieceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande non trouvée avec l'ID: " + id));
        demandePieceRepository.delete(demande);
    }
}