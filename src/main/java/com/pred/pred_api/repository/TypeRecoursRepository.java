package com.pred.pred_api.repository;

import com.pred.pred_api.model.TypeRecours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TypeRecoursRepository extends JpaRepository<TypeRecours, Long> {
    Optional<TypeRecours> findByCode(String code);
    List<TypeRecours> findByCategorieAndActifTrue(String categorie);
    List<TypeRecours> findByActifTrue();
    boolean existsByCode(String code);
}