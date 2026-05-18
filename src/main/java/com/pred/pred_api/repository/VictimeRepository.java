// ============================================================
// FICHIER : src/main/java/com/pred/pred_api/repository/VictimeRepository.java
// ============================================================
package com.pred.pred_api.repository;

import com.pred.pred_api.model.Recours;
import com.pred.pred_api.model.Victime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VictimeRepository extends JpaRepository<Victime, Long> {

    List<Victime> findByRecours(Recours recours);

    @Query("SELECT v FROM Victime v WHERE v.recours.id = :recoursId")
    List<Victime> findByRecoursId(@Param("recoursId") Long recoursId);

    long countByRecours(Recours recours);

    Optional<Victime> findByCin(String cin);

    List<Victime> findByCinContaining(String cin);

    void deleteByRecours(Recours recours);

    @Query("DELETE FROM Victime v WHERE v.recours.id = :recoursId")
    void deleteByRecoursId(@Param("recoursId") Long recoursId);
}