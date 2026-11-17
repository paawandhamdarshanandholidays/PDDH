import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { TripService } from '../../core/services/trip.service';
import { BookingService } from '../../core/services/booking.service';
import { IdempotencySubmission } from '../../core/services/idempotency-key';

@Component({
  selector: 'app-darshan-booking',
  imports: [FormsModule, RouterLink, CurrencyPipe],
  templateUrl: './darshan-booking.html',
  styleUrl: './darshan-booking.css',
})
export class DarshanBooking {
  private readonly tripService = inject(TripService);
  private readonly bookingService = inject(BookingService);
  private readonly submission = new IdempotencySubmission();
  readonly trips = this.tripService.trips;
  readonly selectedId = signal('');
  readonly confirmation = signal('');
  readonly error = signal('');
  readonly submitting = signal(false);
  readonly minDate = new Date().toISOString().slice(0, 10);
  name = '';
  phone = '';
  guests = 1;
  startDate = '';
  endDate = '';

  constructor() {
    inject(ActivatedRoute).queryParamMap.subscribe((params) => this.selectedId.set(params.get('trip') ?? ''));
  }

  get selectedTrip() {
    return this.trips().find((trip) => trip.id === this.selectedId());
  }

  submit(): void {
    if (!this.name.trim() || !this.phone.trim() || !this.startDate || !this.endDate || !this.selectedId()) {
      this.error.set('Please complete all fields and choose a journey.');
      return;
    }
    if (this.endDate <= this.startDate) {
      this.error.set('The end date must be after the start date.');
      return;
    }
    this.error.set('');
    this.confirmation.set('');
    this.submitting.set(true);
    const request = {
      bookingType: 'TRIP' as const,
      customerName: this.name.trim(),
      phone: this.phone.trim(),
      guests: this.guests,
      startDate: this.startDate,
      endDate: this.endDate,
      tripId: this.selectedId(),
    };
    this.bookingService.submit(request, this.submission.forPayload(request)).subscribe({
      next: (response) => {
        this.submitting.set(false);
        this.submission.clear();
        this.confirmation.set(`${response?.message || 'Booking request accepted'}${response?.reference ? ` (reference ${response.reference})` : ''}${response?.bookingDate ? ` on ${response.bookingDate}` : ''}. Our team will contact you to confirm the details.`);
        this.name = '';
        this.phone = '';
        this.guests = 1;
        this.startDate = '';
        this.endDate = '';
        this.selectedId.set('');
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        this.error.set(error.status === 409
          ? 'You already submitted these booking details today. Please check your account or try again tomorrow.'
          : error.status
            ? `We could not submit this booking (${error.status}). Please try again or call 7897321190.`
            : 'The booking service could not be reached. Please try again or call 7897321190.');
      },
    });
  }
}
