import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';

import { ReservationFormComponent } from './reservation-form.component';

describe('ReservationFormComponent', () => {
  let component: ReservationFormComponent;
  let fixture: ComponentFixture<ReservationFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ ReactiveFormsModule ],
      declarations: [ ReservationFormComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReservationFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should keep the submit disabled until a book and a member are selected', () => {
    expect(component.form.invalid).toBeTrue();

    component.form.patchValue({ livreId: 1 });
    expect(component.form.invalid).toBeTrue();

    component.form.patchValue({ adherentId: 2 });
    expect(component.form.valid).toBeTrue();
  });

  it('should not emit "create" when the form is invalid', () => {
    spyOn(component.create, 'emit');
    component.onSubmit();
    expect(component.create.emit).not.toHaveBeenCalled();
  });

  it('should emit "create" with the selected ids when the form is valid', () => {
    spyOn(component.create, 'emit');
    component.form.setValue({ livreId: 1, adherentId: 2 });
    component.onSubmit();
    expect(component.create.emit).toHaveBeenCalledWith({ livreId: 1, adherentId: 2 });
  });

  it('should reset the form when resetSignal changes after the first change', () => {
    component.form.setValue({ livreId: 1, adherentId: 2 });
    component.resetSignal = 1;
    component.ngOnChanges({
      resetSignal: {
        previousValue: 0,
        currentValue: 1,
        firstChange: false,
        isFirstChange: () => false,
      }
    });
    expect(component.form.value.livreId).toBeNull();
    expect(component.form.value.adherentId).toBeNull();
  });
});
