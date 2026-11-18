import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BookingRecord, BookingStatus } from '../../core/models/booking.model';
import { BookingService } from '../../core/services/booking.service';
import { HotelService } from '../../core/services/hotel.service';
import { TripService } from '../../core/services/trip.service';
import { LoadingSpinner } from '../../shared/components/loading-spinner/loading-spinner';

type BookingSort = 'createdAt' | 'customerName' | 'bookingDate' | 'status';

@Component({
  selector: 'app-ceo-bookings',
  standalone: true,
  imports: [RouterLink, DatePipe, FormsModule, LoadingSpinner],
  styles: [`
    .booking-table-wrap { overflow-x: auto; border: 1px solid var(--line); border-radius: 1rem; background: #fff; }
    .booking-table { width: 100%; min-width: 900px; border-collapse: collapse; }
    .booking-table th, .booking-table td { padding: .85rem 1rem; border-bottom: 1px solid var(--line); text-align: left; vertical-align: top; }
    .booking-table th { color: var(--muted); background: #f8fafc; font-size: .72rem; letter-spacing: .06em; text-transform: uppercase; }
    .booking-table th button { padding: 0; border: 0; background: none; color: inherit; font: inherit; letter-spacing: inherit; text-transform: inherit; cursor: pointer; }
    .booking-table td { color: var(--ink); font-size: .86rem; }
    .booking-table tbody tr:hover { background: #f8fafc; }
    .booking-table__primary { display: block; font-weight: 700; }
    .booking-table__secondary { display: block; margin-top: .2rem; color: var(--muted); font-size: .76rem; }
    .booking-pager { display: flex; align-items: center; justify-content: space-between; gap: 1rem; flex-wrap: wrap; padding: 1rem 0; color: var(--muted); font-size: .85rem; }
    .booking-pager__actions { display: flex; align-items: center; gap: .6rem; }
  `],
  template: `
    <section class="admin-page">
      <div class="admin-heading"><div><p class="eyebrow">Executive workspace</p><h1>Customer bookings</h1>
        <p>Review trip and accommodation requests in sortable pages of up to 50 records.</p>
      </div><a class="text-link" routerLink="/ceo/dashboard">← CEO dashboard</a></div>
      @if (error()) { <div class="notice notice--error" role="alert">{{ error() }}</div> }
      @if (loading()) { <div class="booking-loading"><app-loading-spinner label="Loading bookings" /> Loading bookings…</div> }

      <section class="user-list-panel">
        <div class="list-heading"><div><p class="eyebrow">Travel and service requests</p><h2>{{ total() }} bookings</h2></div>
          <button class="text-button" type="button" (click)="refresh()" [disabled]="loading()">Refresh</button>
        </div>
        <div class="booking-table-wrap">
          <table class="booking-table">
            <thead><tr>
              <th>Reference</th><th><button type="button" (click)="sortBy('customerName')">Customer {{ sortMark('customerName') }}</button></th>
              <th>Trip or stay</th><th>Guests & dates</th><th><button type="button" (click)="sortBy('bookingDate')">Booked {{ sortMark('bookingDate') }}</button></th>
              <th><button type="button" (click)="sortBy('status')">Status {{ sortMark('status') }}</button></th><th>Action</th>
            </tr></thead>
            <tbody>
              @for (booking of bookings(); track booking.id) {
                <tr>
                  <td><span class="booking-table__primary">{{ booking.id }}</span><span class="booking-table__secondary">{{ booking.bookingType }}</span></td>
                  <td><span class="booking-table__primary">{{ booking.customerName }}</span><span class="booking-table__secondary">{{ booking.email }}<br>{{ booking.phone }}</span></td>
                  <td>{{ itemName(booking) }}</td>
                  <td>{{ booking.guests }} guest(s)<span class="booking-table__secondary">{{ booking.startDate | date:'mediumDate' }}{{ booking.endDate ? ' – ' + (booking.endDate | date:'mediumDate') : '' }}</span></td>
                  <td>{{ booking.bookingDate | date:'mediumDate' }}</td>
                  <td><span class="booking-status" [class]="'booking-status booking-status--' + booking.status.toLowerCase()">{{ booking.status }}</span></td>
                  <td>
                    <select [ngModel]="booking.status" [ngModelOptions]="{standalone:true}"
                      [attr.aria-label]="'Status for booking ' + booking.id"
                      [disabled]="updating().has(booking.id)"
                      (ngModelChange)="setStatus(booking, $event)">
                      @for (status of bookingStatuses; track status) { <option [value]="status">{{ status }}</option> }
                    </select>
                  </td>
                </tr>
              } @empty {
                <tr><td class="ops-empty" colspan="7">{{ loading() ? 'Loading bookings…' : 'No bookings found.' }}</td></tr>
              }
            </tbody>
          </table>
        </div>
        <div class="booking-pager">
          <label>Rows per page
            <select name="pageSize" [ngModel]="size()" (ngModelChange)="setPageSize($event)">
              @for (option of pageSizes; track option) { <option [ngValue]="option">{{ option }}</option> }
            </select>
          </label>
          <div class="booking-pager__actions"><span>Page {{ page() + 1 }} of {{ pageCount() }} · {{ total() }} total</span>
            <button class="button button--small" type="button" (click)="changePage(-1)" [disabled]="page() === 0 || loading()">Previous</button>
            <button class="button button--small" type="button" (click)="changePage(1)" [disabled]="page() + 1 >= totalPages() || loading()">Next</button>
          </div>
        </div>
      </section>
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
  readonly page = signal(0);
  readonly size = signal(50);
  readonly total = signal(0);
  readonly totalPages = signal(0);
  readonly sort = signal<BookingSort>('createdAt');
  readonly direction = signal<'ASC' | 'DESC'>('DESC');
  readonly pageSizes = [10, 25, 50];
  readonly updating = signal<Set<string>>(new Set());
  readonly bookingStatuses: BookingStatus[] = ['PENDING', 'CONFIRMED', 'REJECTED', 'CANCELLED'];

  constructor() {
    this.refresh();
  }

  pageCount(): number {
    return Math.max(this.totalPages(), 1);
  }

  sortMark(field: BookingSort): string {
    return this.sort() === field ? (this.direction() === 'ASC' ? '↑' : '↓') : '↕';
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set('');
    this.bookingService.getBookingsPage(this.page(), this.size(), this.sort(), this.direction()).subscribe({
      next: (response) => {
        this.bookings.set(response.content);
        this.total.set(response.totalElements);
        this.totalPages.set(response.totalPages);
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

  setPageSize(size: number): void {
    if (!this.pageSizes.includes(Number(size))) return;
    this.size.set(Number(size));
    this.page.set(0);
    this.refresh();
  }

  changePage(delta: number): void {
    const page = this.page() + delta;
    if (page < 0 || page >= this.totalPages()) return;
    this.page.set(page);
    this.refresh();
  }

  sortBy(field: BookingSort): void {
    this.direction.set(this.sort() === field && this.direction() === 'ASC' ? 'DESC' : 'ASC');
    this.sort.set(field);
    this.page.set(0);
    this.refresh();
  }

  setStatus(booking: BookingRecord, status: BookingStatus): void {
    this.error.set('');
    this.updating.update((current) => new Set(current).add(booking.id));
    this.bookingService.updateStatus(booking.id, status).subscribe({
      next: (updated) => {
        this.bookings.update((current) => current.map((item) => item.id === booking.id ? updated : item));
        this.updating.update((current) => {
          const next = new Set(current);
          next.delete(booking.id);
          return next;
        });
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.status
          ? `Could not update booking ${booking.id} (${error.status}).`
          : 'Could not reach the bookings API to update this reservation.');
        this.updating.update((current) => {
          const next = new Set(current);
          next.delete(booking.id);
          return next;
        });
      },
    });
  }
}
