package com.pred.pred_api.service;

import com.pred.pred_api.model.Notification;
import com.pred.pred_api.model.Recours;
import com.pred.pred_api.model.User;
import com.pred.pred_api.model.enums.StatutRecours;
import com.pred.pred_api.model.enums.TypeNotification;
import com.pred.pred_api.repository.NotificationRepository;
import com.pred.pred_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // ============================================================
    // Création de notifications
    // ============================================================

    public void notifierUtilisateur(User utilisateur, Recours recours,
                                    String titreFr, String messageFr,
                                    String titreAr, String messageAr,
                                    TypeNotification type) {
        Notification notification = Notification.builder()
                .utilisateur(utilisateur)
                .recours(recours)
                .titreFr(titreFr)
                .messageFr(messageFr)
                .titreAr(titreAr)
                .messageAr(messageAr)
                .typeNotification(type)
                .lu(false)
                .build();

        notificationRepository.save(notification);
    }

    public void notifierGreffeNouveauRecours(Recours recours) {
        List<User> greffiers = userRepository.findByRole(com.pred.pred_api.model.enums.Role.GREFFIER);

        String titreFr = "Nouveau recours déposé";
        String messageFr = "Un nouveau recours n° " + recours.getNumeroRecours() + " a été déposé par " +
                recours.getUtilisateur().getFullNameFr();

        String titreAr = "طعن جديد مودع";
        String messageAr = "تم إيداع طعن جديد رقم " + recours.getNumeroRecours() + " من طرف " +
                recours.getUtilisateur().getFullNameAr();

        for (User greffier : greffiers) {
            notifierUtilisateur(greffier, recours, titreFr, messageFr, titreAr, messageAr, TypeNotification.EMAIL);
        }
    }

    public void notifierChangementStatut(Recours recours, StatutRecours ancienStatut, StatutRecours nouveauStatut) {
        String titreFr = "Changement de statut de votre recours";
        String messageFr = "Votre recours n° " + recours.getNumeroRecours() +
                " est passé de \"" + ancienStatut + "\" à \"" + nouveauStatut + "\".";

        String titreAr = "تغيير حالة طعنك";
        String messageAr = "طعنك رقم " + recours.getNumeroRecours() +
                " انتقل من \"" + ancienStatut + "\" إلى \"" + nouveauStatut + "\".";

        notifierUtilisateur(recours.getUtilisateur(), recours, titreFr, messageFr, titreAr, messageAr, TypeNotification.EMAIL);
    }

    public void notifierDecisionRendue(Recours recours) {
        String titreFr = "Décision rendue pour votre recours";
        String messageFr = "Une décision a été rendue pour votre recours n° " + recours.getNumeroRecours() +
                ". Veuillez consulter la plateforme pour plus de détails.";

        String titreAr = "صدور قرار بشأن طعنك";
        String messageAr = "تم إصدار قرار بشأن طعنك رقم " + recours.getNumeroRecours() +
                ". يرجى الاطلاع على المنصة لمزيد من التفاصيل.";

        notifierUtilisateur(recours.getUtilisateur(), recours, titreFr, messageFr, titreAr, messageAr, TypeNotification.EMAIL);
    }

    // ============================================================
    // Gestion des notifications
    // ============================================================

    public List<Notification> getNotificationsUtilisateur(User utilisateur) {
        return notificationRepository.findByUtilisateurOrderByDateEnvoiDesc(utilisateur);
    }

    public List<Notification> getNotificationsNonLues(User utilisateur) {
        return notificationRepository.findByUtilisateurAndLuFalse(utilisateur);
    }

    public void marquerCommeLue(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setLu(true);
            notificationRepository.save(notification);
        });
    }

    public void marquerToutesCommeLues(User utilisateur) {
        List<Notification> notifications = notificationRepository.findByUtilisateurAndLuFalse(utilisateur);
        notifications.forEach(n -> n.setLu(true));
        notificationRepository.saveAll(notifications);
    }

    public long countNotificationsNonLues(User utilisateur) {
        return notificationRepository.countByUtilisateurAndLuFalse(utilisateur);
    }
    // ✅ Supprimer une notification
    public void deleteNotification(Long id, User user) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée"));

        if (!notification.getUtilisateur().getId().equals(user.getId())) {
            throw new RuntimeException("Accès non autorisé");
        }

        notificationRepository.delete(notification);
    }

    // ✅ Supprimer toutes les notifications d'un utilisateur
    public void deleteAllNotifications(User user) {
        notificationRepository.deleteByUtilisateur(user);
    }
}