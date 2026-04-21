package com.pred.pred_api.repository;

import com.pred.pred_api.model.User;
import com.pred.pred_api.model.enums.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByCin(String cin);

    List<User> findByRole(Role role);

    List<User> findByActif(Boolean actif);

    boolean existsByEmail(String email);

    boolean existsByCin(String cin);

    long countByRole(Role role);

    long countByActifTrue();

    long countByActifFalse();

    @Query("SELECT u FROM User u ORDER BY u.dateInscription DESC")
    List<User> findRecentUsers(Pageable pageable);

    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.nomFr) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.prenomFr) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.nomAr) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.prenomAr) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "u.cin LIKE CONCAT('%', :keyword, '%')")
    List<User> searchByKeyword(@Param("keyword") String keyword);
}