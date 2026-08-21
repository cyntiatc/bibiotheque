package com.ibizabroker.bibliotheque.dto;

import javax.validation.constraints.NotNull;

/**
 * Corps de requete de POST /api/reservations.
 * L'entite Reservation n'est jamais exposee directement : dateReservation,
 * dateExpiration et statut sont calcules/imposes cote serveur (RG-04).
 */
public record ReservationRequestDTO(

        @NotNull(message = "L'identifiant du livre est obligatoire")
        Integer livreId,

        @NotNull(message = "L'identifiant de l'adhérent est obligatoire")
        Integer adherentId

) {
}