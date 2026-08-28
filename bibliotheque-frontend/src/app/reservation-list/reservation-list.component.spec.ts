import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReservationListComponent } from './reservation-list.component';

describe('ReservationListComponent', () => {
  let component: ReservationListComponent;
  let fixture: ComponentFixture<ReservationListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ReservationListComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReservationListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show the empty state message when state is "empty"', () => {
    component.state = 'empty';
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Aucune réservation');
  });

  it('should only allow cancelling reservations that are EN_ATTENTE or DISPONIBLE', () => {
    expect(component.isCancellable({ statut: 'EN_ATTENTE' } as any)).toBeTrue();
    expect(component.isCancellable({ statut: 'DISPONIBLE' } as any)).toBeTrue();
    expect(component.isCancellable({ statut: 'ANNULEE' } as any)).toBeFalse();
    expect(component.isCancellable({ statut: 'EXPIREE' } as any)).toBeFalse();
    expect(component.isCancellable({ statut: 'HONOREE' } as any)).toBeFalse();
  });
});
