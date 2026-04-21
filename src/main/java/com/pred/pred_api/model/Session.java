package com.pred.pred_api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "session")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private User utilisateur;

    @Column(nullable = false, unique = true, length = 500)
    private String tokenJwt;

    @CreationTimestamp
    private LocalDateTime dateCreation;

    @Column(nullable = false)
    private LocalDateTime dateExpiration;

    @Column(nullable = false, length = 45)
    private String adresseIp;

    private Boolean actif = true;
}
