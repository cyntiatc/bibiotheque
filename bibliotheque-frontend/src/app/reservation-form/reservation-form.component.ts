import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Book, CreateReservationDto, Member } from '../_model/reservation';

/**
 * Composant de présentation (dumb) : formulaire de création de réservation.
 * Ne fait aucun appel réseau ; se contente d'émettre `create` avec un DTO valide
 * et laisse le conteneur parent gérer l'appel au service et les erreurs.
 */
@Component({
  selector: 'app-reservation-form',
  templateUrl: './reservation-form.component.html',
  styleUrls: ['./reservation-form.component.css']
})
export class ReservationFormComponent implements OnChanges {

  @Input() books: Book[] = [];
  @Input() members: Member[] = [];
  /** Chargement des dropdowns Livre/Adhérent (pas celui de la liste des réservations). */
  @Input() optionsLoading = false;
  @Input() optionsError: string | null = null;
  @Input() isSubmitting = false;
  /** Message d'erreur métier renvoyé par le backend lors de la dernière tentative de création. */
  @Input() errorMessage: string | null = null;
  /** Erreurs de validation champ par champ (400), le cas échéant. */
  @Input() validationErrors: Record<string, string> | null = null;
  /** Incrémenté par le parent après une création réussie : signal de réinitialisation du formulaire. */
  @Input() resetSignal = 0;

  @Output() create = new EventEmitter<CreateReservationDto>();
  @Output() retryOptions = new EventEmitter<void>();

  readonly form: FormGroup = this.fb.group({
    livreId: [null, Validators.required],
    adherentId: [null, Validators.required],
  });

  /** Exposé pour itérer sur `validationErrors` dans le template. */
  readonly objectKeys = Object.keys;

  constructor(private fb: FormBuilder) { }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['resetSignal'] && !changes['resetSignal'].firstChange) {
      this.form.reset();
    }
    if (changes['optionsLoading']) {
      if (this.optionsLoading) {
        this.form.disable();
      } else {
        this.form.enable();
      }
    }
  }

  trackById(_index: number, item: Book | Member): number {
    return item.id;
  }

  onSubmit(): void {
    if (this.form.invalid || this.isSubmitting) {
      this.form.markAllAsTouched();
      return;
    }
    const dto: CreateReservationDto = {
      livreId: this.form.value.livreId,
      adherentId: this.form.value.adherentId,
    };
    this.create.emit(dto);
  }
}
