// ============================================================
// FICHIER : src/main/java/com/pred/pred_api/controller/DossierController.java
// ============================================================
package com.pred.pred_api.controller;

import com.pred.pred_api.model.Dossier;
import com.pred.pred_api.model.User;
import com.pred.pred_api.service.DossierService;
import com.pred.pred_api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dossiers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class DossierController {

    private final DossierService dossierService;
    private final UserService userService;

    // Créer un dossier avec plusieurs affaires
    @PostMapping
    public ResponseEntity<?> createDossier(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> request) {
        try {
            User user = userService.findByEmail(userDetails.getUsername());

            String titreFr = (String) request.getOrDefault("titreFr", "Dossier");
            String titreAr = (String) request.getOrDefault("titreAr", "ملف");
            List<Map<String, Object>> affairesData = (List<Map<String, Object>>) request.get("affaires");

            if (affairesData == null || affairesData.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Aucune affaire fournie"));
            }

            Map<String, Object> result = dossierService.createDossierWithAffaires(user, titreFr, titreAr, affairesData);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Récupérer les dossiers de l'utilisateur
    @GetMapping
    public ResponseEntity<List<Dossier>> getMyDossiers(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(dossierService.findByUtilisateur(user.getId()));
    }

    // Récupérer un dossier par ID
    @GetMapping("/{id}")
    public ResponseEntity<Dossier> getDossier(@PathVariable Long id) {
        return ResponseEntity.ok(dossierService.findById(id));
    }
}