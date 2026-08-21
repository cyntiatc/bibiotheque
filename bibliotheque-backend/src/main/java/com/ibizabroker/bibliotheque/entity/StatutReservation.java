package com.ibizabroker.bibliotheque.entity;

/**
 * Cycle de vie d'une {@link Reservation}.
 * EN_ATTENTE et DISPONIBLE sont les seuls statuts "actifs" (voir RG-02, RG-03,
 * RG-05, RG-06 dans ReservationService) ; ANNULEE, EXPIREE et HONOREE sont
 * des statuts terminaux.
 */
public enum StatutReservation {
    EN_ATTENTE,
    DISPONIBLE,
    ANNULEE,
    EXPIREE,
    HONOREE
}