package com.pred.pred_api.service;

import com.pred.pred_api.dto.DemandePieceRequest;
import com.pred.pred_api.dto.DemandePieceResponse;
import com.pred.pred_api.exception.ResourceNotFoundException;
import com.pred.pred_api.model.DemandePiece;
import com.pred.pred_api.model.Recours;
import com.pred.pred_api.model.User;
import com.pred.pred_api.model.enums.StatutDemandePiece;
import com.pred.pred_api.model.enums.TypeNotification;  // ← CORRECTION ICI
import com.pred.pred_api.repository.DemandePieceRepository;
import com.pred.pred_api.repository.RecoursRepository;
import com.pred.pred_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DemandePieceService {

    private final DemandePieceRepository demandePieceRepository;
    private final RecoursRepository recoursRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // Remplacer la méthode createDemande

    @Transactional
    public DemandePieceResponse createDemande(Long recoursId, Long greffierId, DemandePieceRequest request) {
        Recours recours = recoursRepository.findById(recoursId)
                .orElseThrow(() -> new ResourceNotFoundException("Recours non trouvé"));

        User greffier = userRepository.findById(greffierId)
                .orElseThrow(() -> new ResourceNotFoundException("Greffier non trouvé"));

        DemandePiece demande = DemandePiece.builder()
                .recours(recours)
                .greffier(greffier)
                .message(request.getMessageFr()) // Correction: message au lieu de messageFr
                .delaiJour(request.getDelaiJour())
                .dateEnvoi(LocalDateTime.now())
                .dateExpiration(LocalDateTime.now().plusDays(request.getDelaiJour()))
                .statut(StatutDemandePiece.EN_ATTENTE)
                .build();

        DemandePiece saved = demandePieceRepository.save(demande);

        // Notification au justiciable
        notificationService.notifierUtilisateur(recours.getUtilisateur(), recours,
                "Demande de pièces complémentaires",
                request.getMessageFr(),
                "طلب وثائق إضافية",
                request.getMessageAr(),
                TypeNotification.EMAIL);  // ← TypeNotification au lieu de TypeError

        return toResponse(saved);
    }



    public List<DemandePieceResponse> findByRecours(Long recoursId) {
        Recours recours = recoursRepository.findById(recoursId)
                .orElseThrow(() -> new ResourceNotFoundException("Recours non trouvé"));

        return demandePieceRepository.findByRecours(recours).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<DemandePieceResponse> findEnAttente() {
        return demandePieceRepository.findByStatut(StatutDemandePiece.EN_ATTENTE).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<DemandePieceResponse> findExpirees() {
        return demandePieceRepository.findExpirees(LocalDateTime.now()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<DemandePieceResponse> findUrgentes() {
        LocalDateTime dans3Jours = LocalDateTime.now().plusDays(3);
        return demandePieceRepository.findByStatutAndDateExpirationBefore(
                        StatutDemandePiece.EN_ATTENTE, dans3Jours).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void marquerSatisfaite(Long id) {
        DemandePiece demande = demandePieceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande non trouvée"));

        demande.setStatut(StatutDemandePiece.SATISFAITE);
        demandePieceRepository.save(demande);
    }

    public long countEnAttente() {
        return demandePieceRepository.countByStatut(StatutDemandePiece.EN_ATTENTE);
    }

    public long countExpirees() {
        return demandePieceRepository.countExpirees(LocalDateTime.now());
    }

    // Mettre à jour toResponse
    private DemandePieceResponse toResponse(DemandePiece demande) {
        long joursRestants = ChronoUnit.DAYS.between(LocalDateTime.now(), demande.getDateExpiration());

        // Extraire messageFr et messageAr du message (stocké en JSON ou format spécial)
        String messageFr = demande.getMessage();
        String messageAr = demande.getMessage();

        // Si le message est stocké au format "FR||AR"
        if (demande.getMessage() != null && demande.getMessage().contains("||")) {
            String[] parts = demande.getMessage().split("\\|\\|");
            messageFr = parts[0];
            messageAr = parts.length > 1 ? parts[1] : parts[0];
        }

        return DemandePieceResponse.builder()
                .id(demande.getId())
                .recoursId(demande.getRecours().getId())
                .recoursNumero(demande.getRecours().getNumeroRecours())
                .greffierId(demande.getGreffier().getId())
                .greffierNom(demande.getGreffier().getNomFr())
                .greffierPrenom(demande.getGreffier().getPrenomFr())
                .messageFr(messageFr)
                .messageAr(messageAr)
                .delaiJour(demande.getDelaiJour())
                .dateEnvoi(demande.getDateEnvoi())
                .dateExpiration(demande.getDateExpiration())
                .statut(demande.getStatut().name())
                .estExpiree(demande.getDateExpiration().isBefore(LocalDateTime.now()))
                .joursRestants(joursRestants > 0 ? joursRestants : 0)
                .build();
    }
}