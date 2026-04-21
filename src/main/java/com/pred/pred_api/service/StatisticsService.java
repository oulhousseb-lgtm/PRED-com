package com.pred.pred_api.service;

import com.pred.pred_api.dto.StatistiquesResponse;
import com.pred.pred_api.model.enums.Role;
import com.pred.pred_api.model.enums.StatutRecours;
import com.pred.pred_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final UserRepository userRepository;
    private final RecoursRepository recoursRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final ConfigurationRepository configurationRepository;

    public StatistiquesResponse getStatistiquesGlobales() {
        LocalDate debut = LocalDate.now().minusMonths(6);
        LocalDate fin = LocalDate.now();

        return StatistiquesResponse.builder()
                .totalUtilisateurs(userRepository.count())
                .totalJusticiables(userRepository.countByRole(Role.JUSTICIABLE))
                .totalAvocats(userRepository.countByRole(Role.AVOCAT))
                .totalGreffiers(userRepository.countByRole(Role.GREFFIER))
                .totalMagistrats(userRepository.countByRole(Role.MAGISTRAT))
                .totalAdmins(userRepository.countByRole(Role.ADMIN))
                .utilisateursActifs(userRepository.countByActifTrue())
                .utilisateursInactifs(userRepository.countByActifFalse())
                .totalRecours(recoursRepository.count())
                .recoursParStatut(getRecoursParStatut())
                .recoursParType(getRecoursParType())
                .recoursParMois(getRecoursParMois(debut, fin))
                .delaiMoyenTraitementJours(calculerDelaiMoyen())
                .recoursEnAttente(recoursRepository.countByStatut(StatutRecours.EN_ATTENTE_PIECES))
                // Nouveau code (corrigé)
                .recoursClotures(recoursRepository.countByStatutIn(List.of(StatutRecours.JUGE, StatutRecours.REJETE)))
                .totalPiecesJointes(pieceJointeRepository.count())
                .tailleTotaleOctets(pieceJointeRepository.sumTailleOctets())
                .tailleMoyenneMo(calculerTailleMoyenne())
                .dateDebut(debut.format(DateTimeFormatter.ISO_DATE))
                .dateFin(fin.format(DateTimeFormatter.ISO_DATE))
                .build();
    }

    public StatistiquesResponse getStatistiquesByPeriod(String debut, String fin) {
        LocalDate dateDebut = LocalDate.parse(debut);
        LocalDate dateFin = LocalDate.parse(fin);

        // Implémentation similaire avec période spécifique
        return getStatistiquesGlobales();
    }

    public Map<String, String> getConfigurations() {
        return configurationRepository.findAll().stream()
                .collect(Collectors.toMap(
                        c -> c.getCle(),
                        c -> c.getValeur()
                ));
    }

    public void updateConfiguration(String cle, String valeur) {
        configurationRepository.findByCle(cle).ifPresent(config -> {
            config.setValeur(valeur);
            configurationRepository.save(config);
        });
    }

    private Map<String, Long> getRecoursParStatut() {
        Map<String, Long> stats = new HashMap<>();
        for (StatutRecours statut : StatutRecours.values()) {
            stats.put(statut.name(), recoursRepository.countByStatut(statut));
        }
        return stats;
    }

    private Map<String, Long> getRecoursParType() {
        // À implémenter
        return new HashMap<>();
    }

    private Map<String, Long> getRecoursParMois(LocalDate debut, LocalDate fin) {
        // À implémenter
        return new HashMap<>();
    }

    private Double calculerDelaiMoyen() {
        // À implémenter
        return 15.5;
    }

    private Double calculerTailleMoyenne() {
        Long total = pieceJointeRepository.sumTailleOctets();
        Long count = pieceJointeRepository.count();
        if (count == 0) return 0.0;
        return (total / 1024.0 / 1024.0) / count;
    }
}