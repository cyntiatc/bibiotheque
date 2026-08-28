import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ReactiveFormsModule } from '@angular/forms';

import { ReservationContainerComponent } from './reservation-container.component';
import { ReservationListComponent } from '../reservation-list/reservation-list.component';
import { ReservationFormComponent } from '../reservation-form/reservation-form.component';

describe('ReservationContainerComponent', () => {
  let component: ReservationContainerComponent;
  let fixture: ComponentFixture<ReservationContainerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ HttpClientTestingModule, ReactiveFormsModule ],
      declarations: [
        ReservationContainerComponent,
        ReservationListComponent,
        ReservationFormComponent,
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReservationContainerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should start in the loading state', () => {
    expect(component.listState).toBe('loading');
  });
});
