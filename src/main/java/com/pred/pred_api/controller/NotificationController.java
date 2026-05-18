package com.pred.pred_api.controller;

import com.pred.pred_api.model.Notification;
import com.pred.pred_api.model.User;
import com.pred.pred_api.service.NotificationService;
import com.pred.pred_api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit) {

        User user = userService.findByEmail(userDetails.getUsername());
        List<Notification> notifications = notificationService.getNotificationsUtilisateur(user);

        List<Map<String, Object>> response = notifications.stream()
                .limit(limit)
                .map(this::toMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Map<String, Object>>> getUnreadNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByEmail(userDetails.getUsername());
        List<Notification> notifications = notificationService.getNotificationsNonLues(user);

        List<Map<String, Object>> response = notifications.stream()
                .map(this::toMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByEmail(userDetails.getUsername());
        long count = notificationService.countNotificationsNonLues(user);

        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable Long id) {
        notificationService.marquerCommeLue(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification marquée comme lue");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByEmail(userDetails.getUsername());
        notificationService.marquerToutesCommeLues(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Toutes les notifications sont marquées comme lues");
        return ResponseEntity.ok(response);
    }

    // ✅ Supprimer une notification
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteNotification(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        User user = userService.findByEmail(userDetails.getUsername());
        notificationService.deleteNotification(id, user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification supprimée");
        return ResponseEntity.ok(response);
    }

    // ✅ Supprimer toutes les notifications
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, String>> deleteAllNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByEmail(userDetails.getUsername());
        notificationService.deleteAllNotifications(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Toutes les notifications sont supprimées");
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> toMap(Notification n) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", n.getId());
        map.put("recoursId", n.getRecours() != null ? n.getRecours().getId() : null);
        map.put("titreFr", n.getTitreFr());
        map.put("messageFr", n.getMessageFr());
        map.put("titreAr", n.getTitreAr());
        map.put("messageAr", n.getMessageAr());
        map.put("lu", n.getLu());
        map.put("dateEnvoi", n.getDateEnvoi().toString());
        map.put("typeNotification", n.getTypeNotification() != null ? n.getTypeNotification().name() : "SYSTEME");
        return map;
    }
}