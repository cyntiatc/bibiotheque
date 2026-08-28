import { Component, OnInit } from '@angular/core';
import { forkJoin } from 'rxjs';
import { ReservationService } from '../_service/reservation.service';
import {
  Book,
  CreateReservationDto,
  Member,
  Reservation,
  ReservationListState,
  ReservationStatus,
} from '../_model/reservation';

/**
 * Composant conteneur (smart) : détient tout l'état de l'écran de réservations
 * et orchestre les appels à ReservationService. Les composants enfants
 * (ReservationListComponent, ReservationFormComponent) sont purement
 * présentationnels et ne font aucun appel réseau.
 */
@Component({
  selector: 'app-reservation-container',
  templateUrl: './reservation-container.component.html',
  styleUrls: ['./reservation-container.component.css']
})
export class ReservationContainerComponent implements OnInit {

  // --- Liste des réservations ---
  reservations: Reservation[] = [];
  listState: ReservationListState = 'loading';
  listErrorMessage = '';
  selectedStatus: ReservationStatus | '' = '';
  cancelErrorMessage: string | null = null;
  cancellingId: number | null = null;

  // --- Formulaire de création ---
  books: Book[] = [];
  members: Member[] = [];
  optionsLoading = true;
  optionsError: string | null = null;
  isSubmitting = false;
  formErrorMessage: string | null = null;
  formValidationErrors: Record<string, string> | null = null;
  /** Incrémenté après chaque création réussie pour signaler au formulaire de se réinitialiser. */
  formResetSignal = 0;

  constructor(private reservationService: ReservationService) { }

  ngOnInit(): void {
    this.loadReservations();
    this.loadFormOptions();
  }

  loadReservations(): void {
    this.listState = 'loading';
    this.reservationService.getReservations(this.selectedStatus).subscribe({
      next: (reservations) => {
        this.reservations = reservations;
        this.listState = reservations.length === 0 ? 'empty' : 'data';
      },
      error: (err) => {
        this.listErrorMessage = this.reservationService.extractErrorMessage(
          err,
          'Le serveur est injoignable, veuillez vérifier votre connexion.'
        );
        this.listState = 'error';
      },
    });
  }

  loadFormOptions(): void {
    this.optionsLoading = true;
    this.optionsError = null;
    forkJoin({
      books: this.reservationService.getBooksForDropdown(),
      members: this.reservationService.getMembersForDropdown(),
    }).subscribe({
      next: ({ books, members }) => {
        this.books = books;
        this.members = members;
        this.optionsLoading = false;
      },
      error: (err) => {
        this.optionsLoading = false;
        this.optionsError = this.reservationService.extractErrorMessage(
          err,
          "Impossible de charger les listes de livres et d'adhérents."
        );
      },
    });
  }

  onStatusFilterChange(status: ReservationStatus | ''): void {
    this.selectedStatus = status;
    this.loadReservations();
  }

  onCreateReservation(dto: CreateReservationDto): void {
    this.isSubmitting = true;
    this.formErrorMessage = null;
    this.formValidationErrors = null;

    this.reservationService.createReservation(dto).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.formResetSignal++;
        // Rafraîchit la liste en arrière-plan, sans recharger la page.
        this.loadReservations();
      },
      error: (err) => {
        this.isSubmitting = false;
        this.formErrorMessage = this.reservationService.extractErrorMessage(err);
        this.formValidationErrors = this.reservationService.extractValidationErrors(err);
      },
    });
  }

  onCancelReservation(reservation: Reservation): void {
    this.cancelErrorMessage = null;
    this.cancellingId = reservation.id;

    this.reservationService.cancelReservation(reservation.id).subscribe({
      next: (updated) => {
        this.cancellingId = null;
        // Met à jour le statut dans la liste immédiatement, sans tout recharger.
        this.reservations = this.reservations.map(r => r.id === reservation.id ? updated : r);
      },
      error: (err) => {
        this.cancellingId = null;
        this.cancelErrorMessage = this.reservationService.extractErrorMessage(err);
      },
    });
  }
}
