import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { provideRouter } from '@angular/router';

import { BookingService } from '../../core/services/booking.service';
import { TripService } from '../../core/services/trip.service';
import { DarshanBooking } from './darshan-booking';

describe('DarshanBooking', () => {
  let component: DarshanBooking;
  let fixture: ComponentFixture<DarshanBooking>;
  let bookingService: { submit: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    bookingService = { submit: vi.fn(() => of({ reference: 'booking-1' })) };
    await TestBed.configureTestingModule({
      imports: [DarshanBooking],
      providers: [
        provideRouter([]),
        { provide: BookingService, useValue: bookingService },
        { provide: TripService, useValue: { trips: signal([{ id: 'trip-1' }]) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DarshanBooking);
    component = fixture.componentInstance;
    await fixture.whenStable();
    component.selectedId.set('trip-1');
    component.name = 'Customer';
    component.phone = '9876543210';
    component.guests = 2;
    component.startDate = '2030-01-10';
    component.endDate = '2030-01-12';
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('clears booking fields after a successful submission', () => {
    component.submit();

    expect(component.name).toBe('');
    expect(component.phone).toBe('');
    expect(component.guests).toBe(1);
    expect(component.startDate).toBe('');
    expect(component.endDate).toBe('');
    expect(component.selectedId()).toBe('');
    expect(component.confirmation()).toContain('booking-1');
  });

  it('explains that identical booking details cannot be submitted again that day', () => {
    bookingService.submit.mockReturnValue(throwError(
      () => new HttpErrorResponse({ status: 409 }),
    ));

    component.submit();

    expect(component.error()).toContain('already submitted these booking details today');
    expect(component.name).toBe('Customer');
  });
});
