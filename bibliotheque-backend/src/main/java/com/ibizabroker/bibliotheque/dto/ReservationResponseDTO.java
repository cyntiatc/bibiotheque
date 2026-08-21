package com.ibizabroker.bibliotheque.dto;

import com.ibizabroker.bibliotheque.entity.StatutReservation;

import java.time.LocalDateTime;

/**
 * Vue exposee au client pour une Reservation.
 */
public record ReservationResponseDTO(

        Integer id,
        Integer livreId,
        String livreNom,
        Integer adherentId,
        String adherentNom,
        LocalDateTime dateReservation,
        LocalDateTime dateExpiration,
        StatutReservation statut

) {
}