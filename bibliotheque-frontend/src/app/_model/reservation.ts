/**
 * Statuts possibles d'une réservation, tels que renvoyés par le backend
 * (enum `StatutReservation` côté Java).
 */
export type ReservationStatus =
  | 'EN_ATTENTE'
  | 'DISPONIBLE'
  | 'ANNULEE'
  | 'EXPIREE'
  | 'HONOREE';

/** Réservation telle que renvoyée par l'API (`ReservationResponseDTO`). */
export interface Reservation {
  id: number;
  livreId: number;
  livreNom: string;
  adherentId: number;
  adherentNom: string;
  /** Chaîne ISO (LocalDateTime backend), consommable directement par le pipe `date`. */
  dateReservation: string;
  dateExpiration: string;
  statut: ReservationStatus;
}

/** Corps envoyé à `POST /api/reservations` (`ReservationRequestDTO`). */
export interface CreateReservationDto {
  livreId: number;
  adherentId: number;
}

/** Livre tel qu'exposé au formulaire de réservation (issu de la liste des livres existante). */
export interface Book {
  id: number;
  nom: string;
}

/** Adhérent tel qu'exposé au formulaire de réservation (issu de la liste des utilisateurs existante). */
export interface Member {
  id: number;
  nom: string;
}

/** Corps d'erreur uniforme renvoyé par le backend (`ApiError` / `GlobalExceptionHandler`). */
export interface ApiError {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
  validationErrors?: Record<string, string> | null;
}

/** Les 4 états d'affichage de l'écran de liste des réservations. */
export type ReservationListState = 'loading' | 'data' | 'empty' | 'error';

export interface ReservationStatusOption {
  value: ReservationStatus;
  label: string;
}

/** Options du filtre de statut, dans l'ordre du cycle de vie d'une réservation. */
export const RESERVATION_STATUSES: ReservationStatusOption[] = [
  { value: 'EN_ATTENTE', label: 'En attente' },
  { value: 'DISPONIBLE', label: 'Disponible' },
  { value: 'ANNULEE', label: 'Annulée' },
  { value: 'EXPIREE', label: 'Expirée' },
  { value: 'HONOREE', label: 'Honorée' },
];

/** Classes Bootstrap du badge de statut (bonus : couleur distinctive par statut). */
export const RESERVATION_STATUS_BADGE_CLASS: Record<ReservationStatus, string> = {
  EN_ATTENTE: 'bg-warning text-dark',
  DISPONIBLE: 'bg-success',
  ANNULEE: 'bg-danger',
  EXPIREE: 'bg-danger',
  HONOREE: 'bg-primary',
};

/** Libellés français affichés dans le badge de statut. */
export const RESERVATION_STATUS_LABEL: Record<ReservationStatus, string> = {
  EN_ATTENTE: 'En attente',
  DISPONIBLE: 'Disponible',
  ANNULEE: 'Annulée',
  EXPIREE: 'Expirée',
  HONOREE: 'Honorée',
};

/** Statuts pour lesquels une réservation peut encore être annulée (RG-05 / RG-06). */
export const CANCELLABLE_STATUSES: ReservationStatus[] = ['EN_ATTENTE', 'DISPONIBLE'];
