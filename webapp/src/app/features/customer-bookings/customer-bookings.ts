import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BookingRecord } from '../../core/models/booking.model';
import { BookingService } from '../../core/services/booking.service';

type BookingSort = 'createdAt' | 'bookingDate' | 'status';

@Component({
  selector: 'app-customer-bookings',
  standalone: true,
  imports: [DatePipe, FormsModule],
  styles: [`
    .booking-table-wrap { overflow-x: auto; border: 1px solid var(--line); border-radius: 1rem; background: #fff; }
    .booking-table { width: 100%; min-width: 760px; border-collapse: collapse; }
    .booking-table th, .booking-table td { padding: .85rem 1rem; border-bottom: 1px solid var(--line); text-align: left; vertical-align: top; }
    .booking-table th { color: var(--muted); background: #f8fafc; font-size: .72rem; letter-spacing: .06em; text-transform: uppercase; }
    .booking-table th button { padding: 0; border: 0; background: none; color: inherit; font: inherit; letter-spacing: inherit; text-transform: inherit; cursor: pointer; }
    .booking-table td { color: var(--ink); font-size: .86rem; }
    .booking-table__primary { display: block; font-weight: 700; }
    .booking-table__secondary { display: block; margin-top: .2rem; color: var(--muted); font-size: .76rem; }
    .booking-pager { display: flex; align-items: center; justify-content: space-between; gap: 1rem; flex-wrap: wrap; padding: 1rem 0; color: var(--muted); font-size: .85rem; }
    .booking-pager__actions { display: flex; align-items: center; gap: .6rem; }
  `],
  template: `
    <section class="admin-page">
      <div class="admin-heading"><div><p class="eyebrow">Your travel plans</p><h1>My bookings</h1>
        <p>Review your trip and accommodation requests. Your bookings load 50 or fewer at a time.</p>
      </div></div>
      @if (error()) { <div class="notice notice--error" role="alert">{{ error() }}</div> }
      @if (loading()) { <p class="form-hint" role="status">Loading your bookings…</p> }
      <section class="user-list-panel">
        <div class="list-heading"><div><p class="eyebrow">Reservations</p><h2>{{ total() }} bookings</h2></div>
          <button class="text-button" type="button" (click)="load()" [disabled]="loading()">Refresh</button>
        </div>
        <div class="booking-table-wrap">
          <table class="booking-table">
            <thead><tr>
              <th>Reference</th><th>Trip or stay</th><th>Guests and dates</th>
              <th><button type="button" (click)="sortBy('bookingDate')">Booked {{ sortMark('bookingDate') }}</button></th>
              <th><button type="button" (click)="sortBy('status')">Status {{ sortMark('status') }}</button></th>
            </tr></thead>
            <tbody>
              @for (booking of bookings(); track booking.id) {
                <tr>
                  <td><span class="booking-table__primary">{{ booking.id }}</span><span class="booking-table__secondary">{{ booking.bookingType }}</span></td>
                  <td>{{ booking.bookingType === 'TRIP' ? 'Trip reservation' : 'Accommodation reservation' }}</td>
                  <td>{{ booking.guests }} guest(s)<span class="booking-table__secondary">{{ booking.startDate | date:'mediumDate' }}{{ booking.endDate ? ' – ' + (booking.endDate | date:'mediumDate') : '' }}</span></td>
                  <td>{{ booking.bookingDate | date:'mediumDate' }}</td>
                  <td><span class="booking-status" [class]="'booking-status booking-status--' + booking.status.toLowerCase()">{{ booking.status }}</span></td>
                </tr>
              } @empty {
                <tr><td colspan="5">{{ loading() ? 'Loading bookings…' : 'You do not have any bookings yet.' }}</td></tr>
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
export class CustomerBookings {
  private readonly bookingService = inject(BookingService);
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
  private requestId = 0;

  constructor() {
    this.load();
  }

  pageCount(): number {
    return Math.max(this.totalPages(), 1);
  }

  sortMark(field: BookingSort): string {
    return this.sort() === field ? (this.direction() === 'ASC' ? '↑' : '↓') : '↕';
  }

  load(): void {
    const requestId = ++this.requestId;
    this.loading.set(true);
    this.error.set('');
    this.bookingService.getBookingsPage(this.page(), this.size(), this.sort(), this.direction()).subscribe({
      next: (response) => {
        if (requestId !== this.requestId) return;
        this.bookings.set(response.content);
        this.total.set(response.totalElements);
        this.totalPages.set(response.totalPages);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        if (requestId !== this.requestId) return;
        this.error.set(error.status
          ? `Could not load your bookings (${error.status}).`
          : 'Could not reach the bookings API.');
        this.loading.set(false);
      },
    });
  }

  setPageSize(size: number): void {
    if (!this.pageSizes.includes(Number(size))) return;
    this.size.set(Number(size));
    this.page.set(0);
    this.load();
  }

  changePage(delta: number): void {
    const page = this.page() + delta;
    if (page < 0 || page >= this.totalPages()) return;
    this.page.set(page);
    this.load();
  }

  sortBy(field: BookingSort): void {
    this.direction.set(this.sort() === field && this.direction() === 'ASC' ? 'DESC' : 'ASC');
    this.sort.set(field);
    this.page.set(0);
    this.load();
  }
}
