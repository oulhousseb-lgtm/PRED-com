package com.pred.pred_api.repository;

import com.pred.pred_api.model.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfigurationRepository extends JpaRepository<Configuration, Long> {

    Optional<Configuration> findByCle(String cle);

    boolean existsByCle(String cle);

    @Query("SELECT c.valeur FROM Configuration c WHERE c.cle = :cle")
    Optional<String> findValeurByCle(@Param("cle") String cle);

    @Query("SELECT c FROM Configuration c WHERE c.cle LIKE %:keyword%")
    java.util.List<Configuration> searchByKeyword(@Param("keyword") String keyword);

    void deleteByCle(String cle);
}