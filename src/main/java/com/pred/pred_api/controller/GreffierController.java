package com.pred.pred_api.controller;

import com.pred.pred_api.dto.*;
import com.pred.pred_api.model.User;
import com.pred.pred_api.model.enums.StatutRecours;
import com.pred.pred_api.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
// Ajouter ces imports en haut du fichier
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/greffier")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('GREFFIER', 'ADMIN')")
@CrossOrigin(origins = "*", maxAge = 3600)
public class GreffierController {

    private final RecoursService recoursService;
    private final UserService userService;
    private final DemandePieceService demandePieceService;
    private final NotificationService notificationService;

    // ============================================================
    // Gestion des recours
    // ============================================================

    @GetMapping("/recours")
    public ResponseEntity<List<RecoursResponse>> getAllRecours() {
        return ResponseEntity.ok(recoursService.findAll());
    }

    @GetMapping("/recours/en-attente")
    public ResponseEntity<List<RecoursResponse>> getRecoursEnAttente() {
        return ResponseEntity.ok(recoursService.findByStatut(StatutRecours.DEPOSE));
    }

    @GetMapping("/recours/{id}")
    public ResponseEntity<RecoursResponse> getRecoursDetail(@PathVariable Long id) {
        return ResponseEntity.ok(recoursService.findDetailedById(id));
    }

    @PutMapping("/recours/{id}/statut")
    public ResponseEntity<RecoursResponse> changerStatut(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody ChangerStatutRequest request) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(recoursService.changerStatut(id, request, user));
    }

    @PutMapping("/recours/{id}/notes")
    public ResponseEntity<Map<String, String>> ajouterNotesInternes(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        recoursService.ajouterNotesInternes(id, request.get("notes"));
        Map<String, String> response = new HashMap<>();
        response.put("message", "Notes ajoutées avec succès");
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // Demandes de pièces complémentaires
    // ============================================================

    @PostMapping("/recours/{id}/demande-pieces")
    public ResponseEntity<DemandePieceResponse> demanderPieces(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody DemandePieceRequest request) {
        User greffier = userService.findByEmail(userDetails.getUsername());
        DemandePieceResponse response = demandePieceService.createDemande(id, greffier.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/recours/{id}/demandes")
    public ResponseEntity<List<DemandePieceResponse>> getDemandesRecours(@PathVariable Long id) {
        return ResponseEntity.ok(demandePieceService.findByRecours(id));
    }

    @GetMapping("/demandes/en-attente")
    public ResponseEntity<List<DemandePieceResponse>> getDemandesEnAttente() {
        return ResponseEntity.ok(demandePieceService.findEnAttente());
    }

    @GetMapping("/demandes/expirees")
    public ResponseEntity<List<DemandePieceResponse>> getDemandesExpirees() {
        return ResponseEntity.ok(demandePieceService.findExpirees());
    }

    @PutMapping("/demandes/{id}/satisfaire")
    public ResponseEntity<Map<String, String>> marquerDemandeSatisfaite(@PathVariable Long id) {
        demandePieceService.marquerSatisfaite(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Demande marquée comme satisfaite");
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // Notifications
    // ============================================================

    @PostMapping("/notifications/recours/{id}")
    public ResponseEntity<Map<String, String>> envoyerNotification(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        recoursService.envoyerNotificationManuelle(id,
                request.get("titreFr"), request.get("messageFr"),
                request.get("titreAr"), request.get("messageAr"));

        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification envoyée avec succès");
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // Recherche avancée
    // ============================================================

    @GetMapping("/recherche")
    public ResponseEntity<List<RecoursResponse>> rechercheAvancee(
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) String cin,
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String dateDebut,
            @RequestParam(required = false) String dateFin) {

        return ResponseEntity.ok(recoursService.rechercheAvancee(numero, cin, nom, statut, dateDebut, dateFin));
    }

    // ============================================================
    // Statistiques du greffe
    // ============================================================

    @GetMapping("/statistiques")
    public ResponseEntity<Map<String, Object>> getStatistiquesGreffe() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecours", recoursService.countAll());
        stats.put("parStatut", recoursService.getStatistiquesParStatut());
        stats.put("demandesEnAttente", demandePieceService.countEnAttente());
        stats.put("demandesExpirees", demandePieceService.countExpirees());
        stats.put("delaiMoyenTraitement", recoursService.getDelaiMoyenTraitement());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/tableau-de-bord")
    public ResponseEntity<Map<String, Object>> getTableauDeBord() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("recoursEnAttente", recoursService.findByStatut(StatutRecours.DEPOSE));
        dashboard.put("demandesUrgentes", demandePieceService.findUrgentes());
        dashboard.put("recoursRecents", recoursService.findRecentRecours(10));
        dashboard.put("statistiques", getStatistiquesGreffe().getBody());
        return ResponseEntity.ok(dashboard);
    }
}