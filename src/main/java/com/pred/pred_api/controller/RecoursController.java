package com.pred.pred_api.controller;

import com.pred.pred_api.dto.*;
import com.pred.pred_api.exception.ResourceNotFoundException;
import com.pred.pred_api.model.PieceJointe;
import com.pred.pred_api.model.Recours;
import com.pred.pred_api.model.User;
import com.pred.pred_api.model.enums.StatutRecours;
import com.pred.pred_api.model.enums.TypeDocument;
import com.pred.pred_api.repository.PieceJointeRepository;
import com.pred.pred_api.service.RecoursService;
import com.pred.pred_api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recours")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class RecoursController {

    private final RecoursService recoursService;
    private final UserService userService;
    private final PieceJointeRepository pieceJointeRepository;

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

        if (!recours.getDeposantId().equals(user.getId()) && !isAgent(user)) {
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
    // UPLOAD DE PIÈCES
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
        Recours recours = recoursService.findEntityById(id);

        if (!recours.getUtilisateur().getId().equals(user.getId()) && !isAgent(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            TypeDocument type;
            try {
                type = TypeDocument.valueOf(typeDocument.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Type de document invalide: " + typeDocument));
            }

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Fichier vide"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Seuls les fichiers PDF sont acceptés"));
            }

            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "Fichier trop volumineux (max 10 Mo)"));
            }

            String cheminStockage = chemin;
            if (cheminStockage == null || cheminStockage.isEmpty()) {
                String numeroDecision = recours.getNumeroDecisionAttaque();
                if (numeroDecision != null && numeroDecision.contains("/")) {
                    String[] parts = numeroDecision.split("/");
                    if (parts.length == 3) {
                        cheminStockage = parts[2] + "/" + parts[1] + "/" + parts[0] + "/";
                    }
                }
                if (cheminStockage == null || cheminStockage.isEmpty()) {
                    String annee = String.valueOf(java.time.LocalDate.now().getYear());
                    String code = recours.getTypeRecours() != null ? recours.getTypeRecours().getCode() : "AUTRE";
                    cheminStockage = annee + "/" + code + "/" + recours.getId() + "/";
                }
            }

            if (!cheminStockage.endsWith("/")) cheminStockage += "/";
            cheminStockage = cheminStockage.replaceAll("^/+", "");

            PieceJointe piece = recoursService.uploadPiece(id, file, type, descriptionFr, descriptionAr, cheminStockage);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Piece jointe ajoutee avec succes");
            response.put("pieceId", piece.getId());
            response.put("nomFichier", piece.getNomFichier());
            response.put("cheminStockage", piece.getCheminStockage());
            response.put("taille", piece.getTailleOctets());
            response.put("tailleFormatee", piece.getTailleFormatee());
            response.put("dateUpload", piece.getDateUpload().toString());
            response.put("typeDocument", piece.getTypeDocument().name());
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de l'upload du fichier: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur inattendue: " + e.getMessage()));
        }
    }

    // ============================================================
    // ✅ TÉLÉCHARGEMENT DE PIÈCE JOINTE (NOUVEAU)
    // ============================================================
    @GetMapping("/{recoursId}/pieces/{pieceId}/download")
    public ResponseEntity<?> downloadPiece(
            @PathVariable Long recoursId,
            @PathVariable Long pieceId) {

        try {
            PieceJointe piece = pieceJointeRepository.findById(pieceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Pièce non trouvée"));

            if (!piece.getRecours().getId().equals(recoursId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "La pièce n'appartient pas à ce recours"));
            }

            Path filePath = Paths.get(piece.getCheminFichier());

            if (!Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Fichier non trouvé sur le disque"));
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = "application/pdf";

            byte[] fileContent = Files.readAllBytes(filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + piece.getNomFichier() + "\"")
                    .body(fileContent);

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la lecture du fichier"));
        }
    }

    @DeleteMapping("/pieces/{pieceId}")
    public ResponseEntity<Map<String, Object>> deletePiece(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long pieceId) {

        User user = userService.findByEmail(userDetails.getUsername());

        try {
            PieceJointe piece = pieceJointeRepository.findById(pieceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Piece non trouvée avec l'ID: " + pieceId));

            Recours recours = piece.getRecours();

            if (!recours.getUtilisateur().getId().equals(user.getId()) && !isAgent(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès non autorisé"));
            }

            try {
                Path filePath = Paths.get(piece.getCheminFichier());
                if (Files.exists(filePath)) Files.delete(filePath);
            } catch (IOException e) {
                System.err.println("Erreur suppression fichier physique: " + e.getMessage());
            }

            pieceJointeRepository.delete(piece);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Pièce supprimée avec succès");
            response.put("pieceId", pieceId);
            response.put("nomFichier", piece.getNomFichier());
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la suppression: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/pieces")
    public ResponseEntity<List<Map<String, Object>>> getPieces(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        User user = userService.findByEmail(userDetails.getUsername());
        Recours recours = recoursService.findEntityById(id);

        if (!recours.getUtilisateur().getId().equals(user.getId()) && !isAgent(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<PieceJointe> pieces = pieceJointeRepository.findByRecours(recours);

        List<Map<String, Object>> response = pieces.stream().map(piece -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", piece.getId());
            map.put("nomFichier", piece.getNomFichier());
            map.put("cheminStockage", piece.getCheminStockage());
            map.put("typeDocument", piece.getTypeDocument().name());
            map.put("descriptionFr", piece.getDescriptionFr());
            map.put("descriptionAr", piece.getDescriptionAr());
            map.put("taille", piece.getTailleOctets());
            map.put("tailleFormatee", piece.getTailleFormatee());
            map.put("dateUpload", piece.getDateUpload().toString());
            return map;
        }).collect(Collectors.toList());

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