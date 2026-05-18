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

import java.util.*;
// Ajouter ces imports en haut du fichier


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
    // ============================================================
    // إحصائيات متقدمة للكاتب - ADDED
    // ============================================================

    /**
     * الحصول على إحصائيات كاملة مع تفاصيل الطلبات حسب الحالة والشهر
     */
    @GetMapping("/statistiques-completes")
    public ResponseEntity<Map<String, Object>> getStatistiquesCompletes() {
        Map<String, Object> stats = new HashMap<>();

        // 1. إجمالي الطعون
        stats.put("totalRecours", recoursService.countAll());

        // 2. إجمالي الملفات (dossiers)
        stats.put("totalDossiers", recoursService.countAllDossiers());

        // 3. توزيع الطعون حسب الحالة (مع جميع الحالات)
        Map<String, Long> recoursParStatut = new LinkedHashMap<>();
        for (StatutRecours statut : StatutRecours.values()) {
            long count = recoursService.countByStatut(statut);
            // إظهار جميع الحالات حتى لو كانت 0
            recoursParStatut.put(statut.name(), count);
        }
        stats.put("recoursParStatut", recoursParStatut);

        // 4. متوسط مدة المعالجة (بالأيام)
        Double delaiMoyen = recoursService.getDelaiMoyenTraitement();
        stats.put("delaiMoyenTraitement", delaiMoyen != null ? Math.round(delaiMoyen) : 0);

        // 5. الطعون حسب الشهر (آخر 6 أشهر)
        Map<String, Long> recoursParMois = recoursService.getRecoursParMois(6);
        stats.put("recoursParMois", recoursParMois);

        // 6. إحصائيات طلبات الوثائق
        stats.put("demandesEnAttente", demandePieceService.countEnAttente());
        stats.put("demandesExpirees", demandePieceService.countExpirees());
        stats.put("demandesSatisfaites", demandePieceService.countSatisfaites());

        // 7. إحصائيات إضافية
        stats.put("recoursCetteSemaine", recoursService.countRecoursDepuisJours(7));
        stats.put("recoursCeMois", recoursService.countRecoursDepuisJours(30));

        // 8. نسبة القضايا المغلقة
        long totalCloture = recoursService.countByStatut(StatutRecours.JUGE) +
                recoursService.countByStatut(StatutRecours.REJETE);
        double tauxCloture = recoursService.countAll() > 0 ?
                (totalCloture * 100.0 / recoursService.countAll()) : 0;
        stats.put("tauxCloture", Math.round(tauxCloture * 10) / 10.0);

        return ResponseEntity.ok(stats);
    }

    /**
     * إحصائيات حسب الفترة الزمنية
     */
    @GetMapping("/statistiques/period")
    public ResponseEntity<Map<String, Object>> getStatistiquesByPeriod(
            @RequestParam(required = false) String debut,
            @RequestParam(required = false) String fin) {

        java.time.LocalDate dateDebut = debut != null ?
                java.time.LocalDate.parse(debut) :
                java.time.LocalDate.now().minusMonths(6);
        java.time.LocalDate dateFin = fin != null ?
                java.time.LocalDate.parse(fin) :
                java.time.LocalDate.now();

        Map<String, Object> stats = new HashMap<>();
        stats.put("dateDebut", dateDebut.toString());
        stats.put("dateFin", dateFin.toString());
        stats.put("recoursParPeriode", recoursService.countRecoursEntreDates(dateDebut, dateFin));
        stats.put("evolutionParMois", recoursService.getEvolutionParMois(dateDebut, dateFin));

        return ResponseEntity.ok(stats);
    }

    /**
     * إحصائيات سريعة للـ Dashboard
     */
    @GetMapping("/dashboard-stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalRecours", recoursService.countAll());
        stats.put("totalDossiers", recoursService.countAllDossiers());
        stats.put("recoursEnAttente", recoursService.countByStatut(StatutRecours.DEPOSE));
        stats.put("recoursEnCours",
                recoursService.countByStatut(StatutRecours.EN_EXAMEN) +
                        recoursService.countByStatut(StatutRecours.EN_ATTENTE_PIECES)
        );
        stats.put("recoursClotures",
                recoursService.countByStatut(StatutRecours.JUGE) +
                        recoursService.countByStatut(StatutRecours.REJETE)
        );
        stats.put("demandesEnAttente", demandePieceService.countEnAttente());
        stats.put("delaiMoyen", recoursService.getDelaiMoyenTraitement());

        return ResponseEntity.ok(stats);
    }
    // ============================================================
// Demandes de pièces complémentaires - ADDED
// ============================================================

    @GetMapping("/demandes")
    public ResponseEntity<List<DemandePieceResponse>> getAllDemandes() {
        return ResponseEntity.ok(demandePieceService.findAll());
    }

    @GetMapping("/demandes/en-attente")
    public ResponseEntity<List<DemandePieceResponse>> getDemandesEnAttente() {
        return ResponseEntity.ok(demandePieceService.findEnAttente());
    }

    @GetMapping("/demandes/expirees")
    public ResponseEntity<List<DemandePieceResponse>> getDemandesExpirees() {
        return ResponseEntity.ok(demandePieceService.findExpirees());
    }

    @GetMapping("/demandes/urgentes")
    public ResponseEntity<List<DemandePieceResponse>> getDemandesUrgentes() {
        return ResponseEntity.ok(demandePieceService.findUrgentes());
    }

    @PutMapping("/demandes/{id}/satisfaire")
    public ResponseEntity<Map<String, String>> marquerDemandeSatisfaite(@PathVariable Long id) {
        demandePieceService.marquerSatisfaite(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Demande marquée comme satisfaite");
        return ResponseEntity.ok(response);


    }
    // ============================================================
    // Demandes de pièces - Endpoints simplifiés qui fonctionnent toujours
    // ============================================================

    /**
     * Récupère toutes les demandes de pièces (retourne une liste vide si erreur)
     */
    @GetMapping("/demandes/list")
    public ResponseEntity<List<DemandePieceResponse>> getAllDemandesList() {
        try {
            List<DemandePieceResponse> demandes = demandePieceService.findAll();
            return ResponseEntity.ok(demandes != null ? demandes : new ArrayList<>());
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Récupère les demandes en attente (retourne une liste vide si erreur)
     */
    @GetMapping("/demandes/en-attente-simple")
    public ResponseEntity<List<DemandePieceResponse>> getDemandesEnAttenteSimple() {
        try {
            List<DemandePieceResponse> demandes = demandePieceService.findEnAttente();
            return ResponseEntity.ok(demandes != null ? demandes : new ArrayList<>());
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Récupère les demandes expirées (retourne une liste vide si erreur)
     */
    @GetMapping("/demandes/expirees-simple")
    public ResponseEntity<List<DemandePieceResponse>> getDemandesExpireesSimple() {
        try {
            List<DemandePieceResponse> demandes = demandePieceService.findExpirees();
            return ResponseEntity.ok(demandes != null ? demandes : new ArrayList<>());
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Récupère toutes les demandes avec gestion d'erreur
     */
    @GetMapping("/demandes/all")
    public ResponseEntity<List<DemandePieceResponse>> getAllDemandesSafe() {
        try {
            List<DemandePieceResponse> demandes = demandePieceService.findAll();
            return ResponseEntity.ok(demandes != null ? demandes : new ArrayList<>());
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Compte les demandes en attente (retourne 0 si erreur)
     */
    @GetMapping("/demandes/count-en-attente")
    public ResponseEntity<Map<String, Long>> countDemandesEnAttente() {
        Map<String, Long> response = new HashMap<>();
        try {
            long count = demandePieceService.countEnAttente();
            response.put("count", count);
        } catch (Exception e) {
            response.put("count", 0L);
        }
        return ResponseEntity.ok(response);
    }
    // ============================================================
// Demandes de pièces - Endpoints de test
// ============================================================

    @GetMapping("/demandes/test-all")
    public ResponseEntity<List<DemandePieceResponse>> testGetAllDemandes() {
        try {
            List<DemandePieceResponse> demandes = demandePieceService.getAllDemandesDirect();
            System.out.println("Nombre de demandes trouvées: " + (demandes != null ? demandes.size() : 0));
            return ResponseEntity.ok(demandes != null ? demandes : new ArrayList<>());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/demandes/test-by-recours/{recoursId}")
    public ResponseEntity<List<DemandePieceResponse>> testGetDemandesByRecours(@PathVariable Long recoursId) {
        try {
            List<DemandePieceResponse> demandes = demandePieceService.getDemandesByRecoursId(recoursId);
            return ResponseEntity.ok(demandes != null ? demandes : new ArrayList<>());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/demandes/debug")
    public ResponseEntity<Map<String, Object>> debugDemandes() {
        Map<String, Object> result = new HashMap<>();
        try {
            long count = demandePieceService.countEnAttente();
            result.put("count", count);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // ============================================================
// Suppression d'une demande de pièces
// ============================================================

    @DeleteMapping("/demandes/{id}")
    public ResponseEntity<Map<String, String>> deleteDemande(@PathVariable Long id) {
        try {
            demandePieceService.deleteDemande(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Demande supprimée avec succès");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}