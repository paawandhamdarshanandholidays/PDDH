import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BookingRecord, BookingStatus } from '../../core/models/booking.model';
import { ContentService } from '../../core/services/content.service';
import { BookingService } from '../../core/services/booking.service';
import { HotelService } from '../../core/services/hotel.service';
import { TripService } from '../../core/services/trip.service';
import { LoadingSpinner } from '../../shared/components/loading-spinner/loading-spinner';

@Component({
  selector: 'app-ceo-bookings',
  standalone: true,
  imports: [RouterLink, DatePipe, LoadingSpinner],
  template: `
    <section class="admin-page">
      <div class="admin-heading"><div><p class="eyebrow">Executive workspace</p><h1>Customer bookings</h1><p>Review trip and accommodation reservation requests.</p></div><a class="text-link" routerLink="/ceo/dashboard">← CEO dashboard</a></div>
      @if (loading()) { <div class="booking-loading"><app-loading-spinner label="Loading bookings" /> Loading bookings…</div> }
      @if (error()) { <div class="notice notice--error" role="alert">{{ error() }}</div> }
      @if (!loading() && !error()) {
        <div class="booking-admin-list">
          @for (booking of bookings(); track booking.id) {
            <article class="booking-admin-card">
              <div class="booking-admin-card__head"><div><span class="eyebrow">{{ booking.bookingType === 'TRIP' ? 'Trip booking' : 'Stay booking' }} · {{ booking.id }}</span><h2>{{ booking.customerName }}</h2></div><span class="booking-status" [class]="'booking-status booking-status--' + booking.status.toLowerCase()">{{ booking.status }}</span></div>
              <div class="booking-admin-card__details"><span>{{ itemName(booking) }}</span><span>{{ booking.guests }} guest(s)</span><span>{{ booking.startDate | date:'mediumDate' }}{{ booking.endDate ? ' – ' + (booking.endDate | date:'mediumDate') : '' }}</span><a [href]="'mailto:' + booking.email">{{ booking.email }}</a><a [href]="'tel:' + booking.phone">{{ booking.phone }}</a></div>
              @if (booking.status === 'PENDING') {
                <div class="booking-admin-card__actions"><button class="button button--small" type="button" (click)="setStatus(booking, 'CONFIRMED')">Confirm</button><button class="text-button text-button--danger" type="button" (click)="setStatus(booking, 'REJECTED')">Reject</button></div>
              }
            </article>
          } @empty { <div class="empty-state"><h3>No bookings yet</h3><p>New online trip and stay bookings will appear here.</p></div> }
        </div>
      }
    </section>
  `,
})
export class CeoBookings {
  private readonly bookingService = inject(BookingService);
  private readonly tripService = inject(TripService);
  private readonly hotelService = inject(HotelService);
  readonly bookings = signal<BookingRecord[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');

  constructor() {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set('');
    this.bookingService.getBookings().subscribe({
      next: (bookings) => {
        this.bookings.set(bookings);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.status
          ? `Could not load bookings (${error.status}). Confirm the CEO bookings API is available.`
          : 'Could not reach the bookings API.');
        this.loading.set(false);
      },
    });
  }

  itemName(booking: BookingRecord): string {
    if (booking.bookingType === 'TRIP') {
      return this.tripService.trips().find((trip) => trip.id === booking.tripId)?.title ?? 'Trip details';
    }
    return this.hotelService.hotels().find((hotel) => hotel.id === booking.hotelId)?.name ?? 'Accommodation details';
  }

  setStatus(booking: BookingRecord, status: BookingStatus): void {
    this.error.set('');
    this.bookingService.updateStatus(booking.id, status).subscribe({
      next: () => {
        this.bookings.update((current) => current.map((item) => item.id === booking.id ? { ...item, status } : item));
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.status
          ? `Could not update booking ${booking.id} (${error.status}).`
          : 'Could not reach the bookings API to update this reservation.');
      },
    });
  }
}
