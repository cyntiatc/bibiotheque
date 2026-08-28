import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import {
  CANCELLABLE_STATUSES,
  RESERVATION_STATUSES,
  RESERVATION_STATUS_BADGE_CLASS,
  RESERVATION_STATUS_LABEL,
  Reservation,
  ReservationListState,
  ReservationStatus,
  ReservationStatusOption,
} from '../_model/reservation';

/**
 * Composant de présentation (dumb) : tableau des réservations, filtre de statut,
 * pagination et bouton d'annulation (avec confirmation). Ne fait aucun appel
 * réseau : tout est piloté par les @Input()/@Output() du conteneur parent.
 */
@Component({
  selector: 'app-reservation-list',
  templateUrl: './reservation-list.component.html',
  styleUrls: ['./reservation-list.component.css']
})
export class ReservationListComponent implements OnChanges {

  @Input() reservations: Reservation[] = [];
  @Input() state: ReservationListState = 'loading';
  @Input() errorMessage: string | null = null;
  @Input() selectedStatus: ReservationStatus | '' = '';
  /** Erreur renvoyée par le serveur lors d'une tentative d'annulation (409, etc.). */
  @Input() cancelError: string | null = null;
  /** Id de la réservation en cours d'annulation, pour afficher un mini-spinner sur son bouton. */
  @Input() cancellingId: number | null = null;

  @Output() statusFilterChange = new EventEmitter<ReservationStatus | ''>();
  @Output() retry = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<Reservation>();
  @Output() dismissCancelError = new EventEmitter<void>();

  readonly statusOptions: ReservationStatusOption[] = RESERVATION_STATUSES;
  readonly badgeClasses = RESERVATION_STATUS_BADGE_CLASS;
  readonly statusLabels = RESERVATION_STATUS_LABEL;

  readonly pageSize = 10;
  currentPage = 1;

  /** Réservation en attente de confirmation d'annulation (null = pas de modal ouverte). */
  reservationPendingCancel: Reservation | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    // Toute nouvelle liste (changement de filtre, rafraîchissement) repart à la page 1.
    if (changes['reservations']) {
      this.currentPage = 1;
    }
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.reservations.length / this.pageSize));
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  get pagedReservations(): Reservation[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.reservations.slice(start, start + this.pageSize);
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) {
      return;
    }
    this.currentPage = page;
  }

  isCancellable(reservation: Reservation): boolean {
    return CANCELLABLE_STATUSES.includes(reservation.statut);
  }

  trackByReservationId(_index: number, reservation: Reservation): number {
    return reservation.id;
  }

  onFilterClick(status: ReservationStatus | ''): void {
    this.statusFilterChange.emit(status);
  }

  askCancelConfirmation(reservation: Reservation): void {
    this.reservationPendingCancel = reservation;
  }

  closeCancelModal(): void {
    this.reservationPendingCancel = null;
  }

  confirmCancel(): void {
    if (this.reservationPendingCancel) {
      this.cancel.emit(this.reservationPendingCancel);
      this.reservationPendingCancel = null;
    }
  }
}
