import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BooksService } from './books.service';
import { UsersService } from './users.service';
import {
  ApiError,
  Book,
  CreateReservationDto,
  Member,
  Reservation,
  ReservationStatus,
} from '../_model/reservation';

/**
 * Point d'entrée unique pour tous les appels API liés aux réservations.
 * Aucun composant ne doit appeler HttpClient directement : ce service
 * centralise également la traduction des erreurs HTTP en messages lisibles.
 */
@Injectable({
  providedIn: 'root'
})
export class ReservationService {

  private baseURL = 'http://localhost:8080/api/reservations';

  constructor(
    private httpClient: HttpClient,
    private booksService: BooksService,
    private usersService: UsersService
  ) { }

  /** Récupère les réservations, filtrées par statut si un statut est fourni. */
  getReservations(statut?: ReservationStatus | ''): Observable<Reservation[]> {
    let params = new HttpParams();
    if (statut) {
      params = params.set('statut', statut);
    }
    return this.httpClient.get<Reservation[]>(this.baseURL, { params });
  }

  /** Crée une réservation pour le couple (livre, adhérent) choisi dans le formulaire. */
  createReservation(dto: CreateReservationDto): Observable<Reservation> {
    return this.httpClient.post<Reservation>(this.baseURL, dto);
  }

  /** Annule une réservation encore active (`EN_ATTENTE` / `DISPONIBLE` — RG-05/RG-06). */
  cancelReservation(id: number): Observable<Reservation> {
    return this.httpClient.patch<Reservation>(`${this.baseURL}/${id}/annuler`, {});
  }

  /** Livres disponibles pour le dropdown de création (réutilise le service Livres existant). */
  getBooksForDropdown(): Observable<Book[]> {
    return this.booksService.getBooksList().pipe(
      map(books => books.map(b => ({ id: b.bookId, nom: b.bookName })))
    );
  }

  /** Adhérents disponibles pour le dropdown de création (réutilise le service Utilisateurs existant). */
  getMembersForDropdown(): Observable<Member[]> {
    return this.usersService.getUsersList().pipe(
      map(users => users.map(u => ({ id: u.userId, nom: u.name })))
    );
  }

  /**
   * Traduit une erreur HTTP en message lisible pour l'utilisateur.
   * Priorité au message métier renvoyé par le backend (`ApiError.message`,
   * ex. RG-01/RG-02/RG-03), avec un repli explicite pour les cas que le
   * serveur ne documente pas (serveur injoignable, etc.).
   */
  extractErrorMessage(err: unknown, fallback = 'Une erreur inattendue est survenue.'): string {
    if (err instanceof HttpErrorResponse) {
      if (err.status === 0) {
        return 'Le serveur est injoignable, veuillez vérifier votre connexion.';
      }

      const apiError = err.error as ApiError | null;
      if (apiError?.message) {
        return apiError.message;
      }

      switch (err.status) {
        case 400:
          return 'La requête envoyée est invalide.';
        case 404:
          return 'La ressource demandée est introuvable.';
        case 409:
          return 'Cette action entre en conflit avec une règle de gestion.';
        default:
          return fallback;
      }
    }
    return fallback;
  }

  /** Extrait les erreurs de validation champ par champ (400) si le backend en fournit. */
  extractValidationErrors(err: unknown): Record<string, string> | null {
    if (err instanceof HttpErrorResponse && err.status === 400) {
      const apiError = err.error as ApiError | null;
      return apiError?.validationErrors ?? null;
    }
    return null;
  }
}
