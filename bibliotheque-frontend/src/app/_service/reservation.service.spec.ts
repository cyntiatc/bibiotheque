import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HttpErrorResponse } from '@angular/common/http';

import { ReservationService } from './reservation.service';

describe('ReservationService', () => {
  let service: ReservationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ HttpClientTestingModule ]
    });
    service = TestBed.inject(ReservationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should call PATCH /api/reservations/{id}/annuler to cancel a reservation', () => {
    service.cancelReservation(1).subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/reservations/1/annuler');
    expect(req.request.method).toBe('PATCH');
    req.flush({ id: 1, statut: 'ANNULEE' });
  });

  it('should filter by status when provided to getReservations()', () => {
    service.getReservations('EN_ATTENTE').subscribe();

    const req = httpMock.expectOne(
      r => r.url === 'http://localhost:8080/api/reservations' && r.params.get('statut') === 'EN_ATTENTE'
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  describe('extractErrorMessage()', () => {
    it('should report an unreachable server on a network error (status 0)', () => {
      const err = new HttpErrorResponse({ status: 0 });
      expect(service.extractErrorMessage(err)).toBe(
        'Le serveur est injoignable, veuillez vérifier votre connexion.'
      );
    });

    it('should surface the precise business message on a 409 Conflict', () => {
      const err = new HttpErrorResponse({
        status: 409,
        error: { message: 'RG-03 : l\'adhérent id=1 a atteint la limite de 3 réservations actives simultanées.' },
      });
      expect(service.extractErrorMessage(err)).toBe(
        'RG-03 : l\'adhérent id=1 a atteint la limite de 3 réservations actives simultanées.'
      );
    });

    it('should surface the validation message on a 400 Bad Request', () => {
      const err = new HttpErrorResponse({
        status: 400,
        error: { message: 'Validation échouée sur 1 champ(s).' },
      });
      expect(service.extractErrorMessage(err)).toBe('Validation échouée sur 1 champ(s).');
    });

    it('should fall back to a generic 404 message when the backend sends none', () => {
      const err = new HttpErrorResponse({ status: 404 });
      expect(service.extractErrorMessage(err)).toBe('La ressource demandée est introuvable.');
    });
  });

  describe('extractValidationErrors()', () => {
    it('should return the field-level errors on a 400 response', () => {
      const err = new HttpErrorResponse({
        status: 400,
        error: { validationErrors: { livreId: "L'identifiant du livre est obligatoire" } },
      });
      expect(service.extractValidationErrors(err)).toEqual({
        livreId: "L'identifiant du livre est obligatoire",
      });
    });

    it('should return null for non-400 errors', () => {
      const err = new HttpErrorResponse({ status: 409, error: { validationErrors: { a: 'b' } } });
      expect(service.extractValidationErrors(err)).toBeNull();
    });
  });
});
