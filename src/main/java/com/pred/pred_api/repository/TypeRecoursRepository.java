package com.pred.pred_api.repository;

import com.pred.pred_api.model.TypeRecours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TypeRecoursRepository extends JpaRepository<TypeRecours, Long> {

    Optional<TypeRecours> findByCode(String code);

    List<TypeRecours> findByCategorieAndActifTrue(String categorie);

    List<TypeRecours> findByActifTrue();

    boolean existsByCode(String code);

    // ============================================================
    // NOUVEAU : Une seule requête au lieu de N+1
    // ============================================================
    @Query("SELECT tr, COUNT(r) FROM TypeRecours tr " +
            "LEFT JOIN Recours r ON r.typeRecours = tr " +
            "WHERE tr.actif = true " +
            "GROUP BY tr.id, tr.code, tr.categorie, tr.libelleFr, tr.libelleAr, " +
            "tr.descriptionFr, tr.descriptionAr, tr.actif, tr.dateCreation " +
            "ORDER BY tr.code ASC")
    List<Object[]> findAllActiveWithRecoursCount();

    @Query("SELECT COUNT(r) FROM Recours r WHERE r.typeRecours.id = :typeId")
    long countRecoursByTypeId(@Param("typeId") Long typeId);
}