package com.pred.pred_api.controller;

import com.pred.pred_api.dto.*;
import com.pred.pred_api.model.User;
import com.pred.pred_api.model.enums.StatutRecours;
import com.pred.pred_api.model.enums.TypeDocument;
import com.pred.pred_api.service.RecoursService;
import com.pred.pred_api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recours")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class RecoursController {

    private final RecoursService recoursService;
    private final UserService userService;

    // ============================================================
    // CRUD Operations
    // ============================================================

    @PostMapping
    public ResponseEntity<RecoursResponse> createRecours(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody RecoursRequest request) {
        User user = userService.findByEmail(userDetails.getUsername());
        RecoursResponse response = recoursService.createRecours(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<RecoursResponse>> getMyRecours(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        List<RecoursResponse> recours = recoursService.findByUtilisateur(user.getId());
        return ResponseEntity.ok(recours);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecoursResponse> getRecoursById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = userService.findByEmail(userDetails.getUsername());
        RecoursResponse recours = recoursService.findById(id);

        // Vérification des droits d'accès
        if (!recours.getDeposantId().equals(user.getId()) &&
                !isAgent(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(recours);
    }

    @GetMapping("/numero/{numero}")
    public ResponseEntity<RecoursResponse> getRecoursByNumero(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String numero) {
        User user = userService.findByEmail(userDetails.getUsername());
        RecoursResponse recours = recoursService.findByNumeroRecours(numero);

        if (!recours.getDeposantId().equals(user.getId()) && !isAgent(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(recours);
    }

    // ============================================================
    // Gestion des pièces jointes
    // ============================================================

    // ============================================================
// MODIFICATION : RecoursController.java - Remplacer uploadPiece
// ============================================================
    @PostMapping("/{id}/pieces")
    public ResponseEntity<Map<String, Object>> uploadPiece(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("typeDocument") String typeDocument,
            @RequestParam(value = "descriptionFr", required = false) String descriptionFr,
            @RequestParam(value = "descriptionAr", required = false) String descriptionAr,
            @RequestParam(value = "chemin", required = false, defaultValue = "") String chemin) {

        User user = userService.findByEmail(userDetails.getUsername());
        RecoursResponse recours = recoursService.findById(id);

        if (!recours.getDeposantId().equals(user.getId()) && !isAgent(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            TypeDocument type = TypeDocument.valueOf(typeDocument.toUpperCase());
            var piece = recoursService.uploadPiece(id, file, type, descriptionFr, descriptionAr, chemin);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Pièce jointe ajoutée avec succès");
            response.put("pieceId", piece.getId());
            response.put("nomFichier", piece.getNomFichier());
            response.put("cheminStockage", piece.getCheminStockage());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Type de document invalide"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de l'upload du fichier"));
        }
    }

    @DeleteMapping("/pieces/{pieceId}")
    public ResponseEntity<Map<String, String>> deletePiece(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long pieceId) {
        // TODO: Implémenter la suppression
        Map<String, String> response = new HashMap<>();
        response.put("message", "Pièce supprimée avec succès");
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // Gestion des statuts (Greffier/Magistrat)
    // ============================================================

    @PutMapping("/{id}/statut")
    public ResponseEntity<RecoursResponse> changerStatut(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody ChangerStatutRequest request) {

        User user = userService.findByEmail(userDetails.getUsername());

        if (!isAgent(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        RecoursResponse response = recoursService.changerStatut(id, request, user);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/decision")
    public ResponseEntity<RecoursResponse> ajouterDecision(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam("decisionFr") String decisionFr,
            @RequestParam("decisionAr") String decisionAr,
            @RequestParam(value = "fichier", required = false) MultipartFile fichier) {

        User user = userService.findByEmail(userDetails.getUsername());

        if (!isMagistrat(user) && !isAdmin(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            RecoursResponse response = recoursService.ajouterDecision(id, decisionFr, decisionAr, fichier, user);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/audience")
    public ResponseEntity<RecoursResponse> fixerAudience(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        User user = userService.findByEmail(userDetails.getUsername());

        if (!isAgent(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // TODO: Implémenter la fixation d'audience
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // Recherche et filtrage (Agents)
    // ============================================================


    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<RecoursResponse>> getRecoursByStatut(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String statut) {

        User user = userService.findByEmail(userDetails.getUsername());

        if (!isAgent(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            StatutRecours statutEnum = StatutRecours.valueOf(statut.toUpperCase());
            return ResponseEntity.ok(recoursService.findByStatut(statutEnum));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<RecoursResponse>> searchRecours(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) String statut) {

        User user = userService.findByEmail(userDetails.getUsername());

        if (!isAgent(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // TODO: Implémenter la recherche avancée
        return ResponseEntity.ok(recoursService.findAll());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('GREFFIER')")
    public ResponseEntity<List<RecoursResponse>> getAllRecours() {
        List<RecoursResponse> recours = recoursService.findAll();
        return ResponseEntity.ok(recours);
    }

    // ============================================================
    // Statistiques
    // ============================================================

    @GetMapping("/statistiques")
    public ResponseEntity<Map<String, Object>> getStatistiques(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByEmail(userDetails.getUsername());

        Map<String, Object> stats = new HashMap<>();

        if (isAgent(user)) {
            stats.put("total", recoursService.countAll());
            stats.put("parStatut", recoursService.getStatistiquesParStatut());
        } else {
            stats.put("total", recoursService.countByUtilisateur(user.getId()));
            stats.put("parStatut", recoursService.getStatistiquesParStatutForUser(user.getId()));
        }

        return ResponseEntity.ok(stats);
    }

    // ============================================================
    // Méthodes utilitaires
    // ============================================================

    private boolean isAgent(User user) {
        return isGreffier(user) || isMagistrat(user) || isAdmin(user);
    }

    private boolean isGreffier(User user) {
        return user.getRole() == com.pred.pred_api.model.enums.Role.GREFFIER;
    }

    private boolean isMagistrat(User user) {
        return user.getRole() == com.pred.pred_api.model.enums.Role.MAGISTRAT;
    }

    private boolean isAdmin(User user) {
        return user.getRole() == com.pred.pred_api.model.enums.Role.ADMIN;
    }
}