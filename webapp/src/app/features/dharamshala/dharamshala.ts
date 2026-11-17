import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { BookingService } from '../../core/services/booking.service';
import { HotelService } from '../../core/services/hotel.service';
import { HotelCard } from '../../shared/components/cards/hotel-card';
import { IdempotencySubmission } from '../../core/services/idempotency-key';

@Component({
  selector: 'app-dharamshala',
  imports: [FormsModule, HotelCard],
  templateUrl: './dharamshala.html',
  styleUrl: './dharamshala.css',
})
export class Dharamshala {
  private readonly bookingService = inject(BookingService);
  private readonly submission = new IdempotencySubmission();
  private readonly hotelService = inject(HotelService);
  readonly hotels = this.hotelService.hotels;
  readonly sent = signal(false);
  readonly submittedName = signal('');
  readonly error = signal('');
  readonly submitting = signal(false);
  readonly minDate = new Date().toISOString().slice(0, 10);
  name = '';
  email = '';
  phone = '';
  hotelId = '';
  checkIn = '';
  checkOut = '';
  guests = 1;

  submit(): void {
    if (this.checkOut <= this.checkIn) {
      this.error.set('Check-out must be after check-in.');
      return;
    }
    this.error.set('');
    this.sent.set(false);
    this.submitting.set(true);
    const request = {
      bookingType: 'STAY' as const,
      customerName: this.name.trim(),
      email: this.email.trim(),
      phone: this.phone.trim(),
      guests: this.guests,
      startDate: this.checkIn,
      endDate: this.checkOut,
      hotelId: this.hotelId,
    };
    this.bookingService.submit(request, this.submission.forPayload(request)).subscribe({
      next: () => {
        this.submitting.set(false);
        this.submission.clear();
        this.submittedName.set(this.name.trim());
        this.sent.set(true);
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        this.error.set(error.status
          ? `We could not submit this stay booking (${error.status}). Please try again or call 7897321190.`
          : 'The booking service could not be reached. Please try again or call 7897321190.');
      },
    });
  }
}
