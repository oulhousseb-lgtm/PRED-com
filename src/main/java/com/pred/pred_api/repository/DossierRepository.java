// ============================================================
// FICHIER : src/main/java/com/pred/pred_api/repository/DossierRepository.java
// ============================================================
package com.pred.pred_api.repository;

import com.pred.pred_api.model.Dossier;
import com.pred.pred_api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DossierRepository extends JpaRepository<Dossier, Long> {
    List<Dossier> findByUtilisateur(User utilisateur);
    List<Dossier> findByUtilisateurOrderByDateCreationDesc(User utilisateur);
    Optional<Dossier> findByNumeroDossier(String numeroDossier);
    long countByUtilisateur(User utilisateur);
}