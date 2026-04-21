package com.pred.pred_api.repository;

import com.pred.pred_api.model.Recours;
import com.pred.pred_api.model.TypeRecours;
import com.pred.pred_api.model.User;
import com.pred.pred_api.model.enums.StatutRecours;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecoursRepository extends JpaRepository<Recours, Long> {

    List<Recours> findByUtilisateur(User utilisateur);

    List<Recours> findByStatut(StatutRecours statut);

    List<Recours> findByUtilisateurAndStatut(User utilisateur, StatutRecours statut);

    List<Recours> findByTypeRecours(TypeRecours typeRecours);

    long countByTypeRecours(TypeRecours typeRecours);

    long countByStatut(StatutRecours statut);

    long countByUtilisateur(User utilisateur);

    @Query("SELECT COUNT(r) FROM Recours r WHERE r.statut IN :statuts")
    long countByStatutIn(@Param("statuts") List<StatutRecours> statuts);

    // Méthode utilitaire pour countByStatutIn avec varargs
    default long countByStatutIn(StatutRecours... statuts) {
        return countByStatutIn(List.of(statuts));
    }

    @Query("SELECT COUNT(r) FROM Recours r WHERE YEAR(r.dateDepot) = :annee")
    long countByAnnee(@Param("annee") int annee);

    @Query("SELECT r FROM Recours r WHERE r.numeroRecours = :numero")
    Optional<Recours> findByNumeroRecours(@Param("numero") String numero);

    @Query("SELECT r FROM Recours r ORDER BY r.dateDepot DESC")
    List<Recours> findRecentRecours(Pageable pageable);

    @Query("SELECT r.statut, COUNT(r) FROM Recours r WHERE r.utilisateur = :utilisateur GROUP BY r.statut")
    List<Object[]> countByStatutGroupByStatutForUser(@Param("utilisateur") User utilisateur);
}