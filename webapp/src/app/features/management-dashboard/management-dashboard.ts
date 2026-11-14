import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { BookingRecord, BookingStatus } from '../../core/models/booking.model';
import { BookingService } from '../../core/services/booking.service';
import { EnquiryRecord, EnquiryService, EnquiryStatus } from '../../core/services/enquiry.service';

type DashboardSection = 'ALL' | 'BOOKINGS' | 'ENQUIRIES';

@Component({
  selector: 'app-management-dashboard',
  standalone: true,
  imports: [DatePipe, FormsModule],
  styles: [`.enquiry-message { white-space: pre-wrap; overflow-wrap: anywhere; }`],
  template: `
    <section class="admin-page">
      <div class="admin-heading"><div><p class="eyebrow">Operations workspace</p><h1>Bookings, enquiries & contact</h1><p>Manage bookings, customer enquiries, and Contact Us submissions in one place.</p></div>
        <button class="text-button" type="button" (click)="refresh()" [disabled]="loading()">Refresh</button>
      </div>
      @if (error()) { <div class="notice notice--error" role="alert">{{ error() }}</div> }
      <div class="profile-browser">
        <label>Search<input type="search" name="search" [ngModel]="search()" (ngModelChange)="search.set($event)" placeholder="Customer, email, phone, message…"></label>
        <label>Record type<select name="section" [ngModel]="section()" (ngModelChange)="setSection($event)">
          <option value="ALL">Bookings and enquiries</option><option value="BOOKINGS">Bookings</option><option value="ENQUIRIES">Enquiries & Contact Us</option>
        </select></label>
      </div>
      @if (loading()) { <div class="notice" role="status">Loading bookings and enquiries…</div> }

      @if (section() !== 'ENQUIRIES') {
        <section class="user-list-panel">
          <div class="list-heading"><div><p class="eyebrow">Travel and service requests</p><h2>Bookings <span class="admin-count">{{ filteredBookings().length }}</span></h2></div>
            <label>Status<select name="bookingStatus" [ngModel]="bookingStatus()" (ngModelChange)="setBookingStatusFilter($event)">
              <option value="ALL">All statuses</option><option>PENDING</option><option>CONFIRMED</option><option>REJECTED</option><option>CANCELLED</option>
            </select></label>
          </div>
          <div class="booking-admin-list">
            @for (booking of filteredBookings(); track booking.id) {
              <article class="booking-admin-card">
                <div class="booking-admin-card__head"><div><span class="eyebrow">{{ booking.bookingType }} booking · {{ booking.id }}</span><h3>{{ booking.customerName }}</h3></div>
                  <select [ngModel]="booking.status" [ngModelOptions]="{standalone:true}" [attr.aria-label]="'Status for booking ' + booking.id"
                    (ngModelChange)="setBookingStatus(booking, $event)">
                    @for (status of bookingStatuses; track status) { <option [value]="status">{{ status }}</option> }
                  </select>
                </div>
                <div class="booking-admin-card__details"><span>{{ booking.guests }} guest(s)</span><span>{{ booking.startDate | date:'mediumDate' }}{{ booking.endDate ? ' – ' + (booking.endDate | date:'mediumDate') : '' }}</span>
                  <a [href]="'mailto:' + booking.email">{{ booking.email }}</a><a [href]="'tel:' + booking.phone">{{ booking.phone }}</a>
                </div>
                <button class="text-button" type="button" (click)="toggleBooking(booking.id)">
                  {{ expandedBookingId() === booking.id ? 'Hide details' : 'View full details' }}
                </button>
                @if (expandedBookingId() === booking.id) {
                  <div class="booking-admin-card__details"><span>Booking date: {{ booking.bookingDate | date:'mediumDate' }}</span><span>Customer account: {{ booking.customerId }}</span>
                    @if (booking.tripId) { <span>Trip ID: {{ booking.tripId }}</span> } @if (booking.hotelId) { <span>Stay ID: {{ booking.hotelId }}</span> }
                  </div>
                }
              </article>
            } @empty { @if (!loading()) { <p class="form-hint">No bookings match these filters.</p> } }
          </div>
        </section>
      }

      @if (section() !== 'BOOKINGS') {
        <section class="user-list-panel">
          <div class="list-heading"><div><p class="eyebrow">Customer messages</p><h2>Enquiries & Contact Us <span class="admin-count">{{ filteredEnquiries().length }}</span></h2></div>
            <label>Status<select name="enquiryStatus" [ngModel]="enquiryStatus()" (ngModelChange)="setEnquiryStatusFilter($event)">
              <option value="ALL">All statuses</option><option>OPEN</option><option>IN_PROGRESS</option><option>RESOLVED</option><option>CLOSED</option>
            </select></label>
          </div>
          @for (enquiry of filteredEnquiries(); track enquiry.id) {
            <article class="booking-admin-card">
              <div class="booking-admin-card__head"><div><span class="eyebrow">Contact Us / service enquiry · {{ enquiry.createdAt | date:'medium' }}</span><h3>{{ enquiry.subject }}</h3></div>
                <select [ngModel]="enquiry.status" [ngModelOptions]="{standalone:true}" [attr.aria-label]="'Status for enquiry ' + enquiry.id"
                  (ngModelChange)="setEnquiryStatus(enquiry, $event)">
                  @for (status of enquiryStatuses; track status) { <option [value]="status">{{ status.replace('_', ' ') }}</option> }
                </select>
              </div>
              <div class="booking-admin-card__details"><span>{{ enquiry.name }}</span><a [href]="'mailto:' + enquiry.email">{{ enquiry.email }}</a><a [href]="'tel:' + enquiry.phone">{{ enquiry.phone }}</a></div>
              <button class="text-button" type="button" (click)="toggleEnquiry(enquiry.id)">
                {{ expandedEnquiryId() === enquiry.id ? 'Hide message' : 'View full message' }}
              </button>
              @if (expandedEnquiryId() === enquiry.id) { <p class="form-hint enquiry-message">{{ enquiry.message }}</p> }
            </article>
          } @empty { @if (!loading()) { <p class="form-hint">No enquiries or contact messages match these filters.</p> } }
        </section>
      }
    </section>
  `,
})
export class ManagementDashboard {
  private readonly bookingService = inject(BookingService);
  private readonly enquiryService = inject(EnquiryService);
  readonly bookings = signal<BookingRecord[]>([]);
  readonly enquiries = signal<EnquiryRecord[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly search = signal('');
  readonly section = signal<DashboardSection>('ALL');
  readonly bookingStatus = signal<'ALL' | BookingStatus>('ALL');
  readonly enquiryStatus = signal<'ALL' | EnquiryStatus>('ALL');
  readonly expandedBookingId = signal<string | null>(null);
  readonly expandedEnquiryId = signal<string | null>(null);
  readonly bookingStatuses: BookingStatus[] = ['PENDING', 'CONFIRMED', 'REJECTED', 'CANCELLED'];
  readonly enquiryStatuses: EnquiryStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
  readonly filteredBookings = computed(() => {
    const query = this.search().trim().toLowerCase();
    return this.bookings().filter((booking) =>
      (this.bookingStatus() === 'ALL' || booking.status === this.bookingStatus())
      && (!query || [
        booking.id, booking.customerId, booking.customerName, booking.email, booking.phone,
        booking.bookingType, booking.status, booking.tripId ?? '', booking.hotelId ?? '',
      ].some((value) => value.toLowerCase().includes(query))));
  });
  readonly filteredEnquiries = computed(() => {
    const query = this.search().trim().toLowerCase();
    return this.enquiries().filter((enquiry) =>
      (this.enquiryStatus() === 'ALL' || enquiry.status === this.enquiryStatus())
      && (!query || [
        enquiry.id, enquiry.name, enquiry.email, enquiry.phone, enquiry.subject, enquiry.message, enquiry.status,
      ].some((value) => value.toLowerCase().includes(query))));
  });
  private readonly updatingBookings = signal<Set<string>>(new Set());
  private readonly updatingEnquiries = signal<Set<string>>(new Set());

  constructor() {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set('');
    forkJoin({
      bookings: this.bookingService.getBookings(),
      enquiries: this.enquiryService.list(),
    }).subscribe({
      next: ({ bookings, enquiries }) => {
        this.bookings.set(bookings);
        this.enquiries.set(enquiries);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.status
          ? `Could not load management records (${error.status}).`
          : 'Could not reach the bookings and enquiries APIs.');
        this.loading.set(false);
      },
    });
  }

  setSection(value: string): void {
    this.section.set(value === 'BOOKINGS' || value === 'ENQUIRIES' ? value : 'ALL');
  }

  setBookingStatusFilter(value: string): void {
    if (value === 'ALL' || isBookingStatus(value)) this.bookingStatus.set(value);
  }

  setEnquiryStatusFilter(value: string): void {
    if (value === 'ALL' || isEnquiryStatus(value)) this.enquiryStatus.set(value);
  }

  setBookingStatus(booking: BookingRecord, requestedStatus: string): void {
    if (!isBookingStatus(requestedStatus)) return;
    const status = requestedStatus;
    if (status === booking.status || this.updatingBookings().has(booking.id)) return;
    this.error.set('');
    this.updatingBookings.update((current) => new Set(current).add(booking.id));
    this.bookingService.updateStatus(booking.id, status).pipe(finalize(() =>
      this.updatingBookings.update((current) => {
        const next = new Set(current);
        next.delete(booking.id);
        return next;
      }))).subscribe({
      next: () => this.bookings.update((items) =>
        items.map((item) => item.id === booking.id ? { ...item, status } : item)),
      error: (error: HttpErrorResponse) => this.error.set(error.status
        ? `Could not update booking ${booking.id} (${error.status}).`
        : 'Could not reach the bookings API to update this record.'),
    });
  }

  setEnquiryStatus(enquiry: EnquiryRecord, requestedStatus: string): void {
    if (!isEnquiryStatus(requestedStatus)) return;
    const status = requestedStatus;
    if (status === enquiry.status || this.updatingEnquiries().has(enquiry.id)) return;
    this.error.set('');
    this.updatingEnquiries.update((current) => new Set(current).add(enquiry.id));
    this.enquiryService.updateStatus(enquiry.id, status).pipe(finalize(() =>
      this.updatingEnquiries.update((current) => {
        const next = new Set(current);
        next.delete(enquiry.id);
        return next;
      }))).subscribe({
      next: (updated) => this.enquiries.update((items) =>
        items.map((item) => item.id === enquiry.id ? updated : item)),
      error: (error: HttpErrorResponse) => this.error.set(error.status
        ? `Could not update enquiry ${enquiry.id} (${error.status}).`
        : 'Could not reach the enquiries API to update this record.'),
    });
  }

  toggleBooking(id: string): void {
    this.expandedBookingId.set(this.expandedBookingId() === id ? null : id);
  }

  toggleEnquiry(id: string): void {
    this.expandedEnquiryId.set(this.expandedEnquiryId() === id ? null : id);
  }
}

function isBookingStatus(value: string): value is BookingStatus {
  return value === 'PENDING' || value === 'CONFIRMED' || value === 'REJECTED' || value === 'CANCELLED';
}

function isEnquiryStatus(value: string): value is EnquiryStatus {
  return value === 'OPEN' || value === 'IN_PROGRESS' || value === 'RESOLVED' || value === 'CLOSED';
}
