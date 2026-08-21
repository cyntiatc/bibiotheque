package com.ibizabroker.bibliotheque.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer reservationId;

    // Obligatoire (RG) : une reservation porte toujours sur un livre.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Books livre;

    // Obligatoire (RG) : une reservation porte toujours sur un adherent
    // (entite utilisateur du projet : Users).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users adherent;

    // Fixee cote serveur a la creation (RG-04), jamais fournie par le client.
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateReservation;

    // Calculee cote serveur = dateReservation + 7 jours (RG-04).
    @Column(nullable = false)
    private LocalDateTime dateExpiration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutReservation statut;

}