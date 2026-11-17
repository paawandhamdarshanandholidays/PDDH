import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { BookingRecord, BookingStatus } from '../../core/models/booking.model';
import { BookingService } from '../../core/services/booking.service';
import { EnquiryRecord, EnquiryService, EnquiryStatus } from '../../core/services/enquiry.service';

type DashboardSection = 'ALL' | 'BOOKINGS' | 'ENQUIRIES';
type BookingSort = 'createdAt' | 'customerName' | 'bookingDate' | 'status';
type EnquirySort = 'createdAt' | 'name' | 'subject' | 'status';

@Component({
  selector: 'app-management-dashboard',
  standalone: true,
  imports: [DatePipe, FormsModule],
  styles: [`
    .ops-summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 1rem; margin: 1.25rem 0; }
    .ops-summary article { padding: 1.1rem 1.25rem; border: 1px solid var(--line); border-radius: 1rem; background: linear-gradient(135deg, #fff, #f8fafc); }
    .ops-summary strong { display: block; color: var(--ink); font-size: 1.65rem; }
    .ops-summary span { color: var(--muted); font-size: .82rem; }
    .ops-toolbar, .ops-table-tools, .ops-pager { display: flex; align-items: end; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
    .ops-toolbar { margin: 1.25rem 0; }
    .ops-toolbar label, .ops-table-tools label { min-width: 190px; }
    .ops-table-wrap { overflow-x: auto; border: 1px solid var(--line); border-radius: 1rem; background: #fff; }
    .ops-table { width: 100%; min-width: 920px; border-collapse: collapse; }
    .ops-table th, .ops-table td { padding: .85rem 1rem; border-bottom: 1px solid var(--line); text-align: left; vertical-align: top; }
    .ops-table th { color: var(--muted); background: #f8fafc; font-size: .72rem; letter-spacing: .06em; text-transform: uppercase; }
    .ops-table th button { padding: 0; border: 0; background: none; color: inherit; font: inherit; letter-spacing: inherit; text-transform: inherit; cursor: pointer; }
    .ops-table td { color: var(--ink); font-size: .86rem; }
    .ops-table tbody tr:hover { background: #f8fafc; }
    .ops-table__primary { font-weight: 700; }
    .ops-table__secondary { display: block; margin-top: .2rem; color: var(--muted); font-size: .76rem; }
    .ops-pager { padding: .9rem 0; color: var(--muted); font-size: .85rem; }
    .ops-pager__actions { display: flex; align-items: center; gap: .6rem; }
    .ops-details { max-width: 52rem; white-space: pre-wrap; overflow-wrap: anywhere; }
    .ops-empty { padding: 2rem; text-align: center; color: var(--muted); }
  `],
  template: `
    <section class="admin-page">
      <div class="admin-heading">
        <div><p class="eyebrow">Operations workspace</p><h1>Bookings, enquiries & contact</h1>
          <p>Review and sort customer requests in focused, 50-record pages. Full messages load only when opened.</p>
        </div>
        <button class="text-button" type="button" (click)="refresh()" [disabled]="isLoading()">Refresh</button>
      </div>
      @if (error()) { <div class="notice notice--error" role="alert">{{ error() }}</div> }

      <div class="ops-summary">
        <article><strong>{{ bookingsTotal() }}</strong><span>Bookings</span></article>
        <article><strong>{{ enquiriesTotal() }}</strong><span>Enquiries & Contact Us</span></article>
      </div>

      <div class="ops-toolbar">
        <label>Record type
          <select name="section" [ngModel]="section()" (ngModelChange)="setSection($event)">
            <option value="ALL">Bookings and enquiries</option>
            <option value="BOOKINGS">Bookings</option>
            <option value="ENQUIRIES">Enquiries & Contact Us</option>
          </select>
        </label>
        <label>Search loaded rows
          <input type="search" name="search" [ngModel]="search()" (ngModelChange)="search.set($event)"
            placeholder="Search this page by customer, email, phone…">
        </label>
      </div>

      @if (section() !== 'ENQUIRIES') {
        <section class="user-list-panel">
          <div class="list-heading ops-table-tools">
            <div><p class="eyebrow">Travel and service requests</p><h2>Bookings <span class="admin-count">{{ bookingsTotal() }}</span></h2></div>
            <label>Status on this page
              <select name="bookingStatus" [ngModel]="bookingStatus()" (ngModelChange)="setBookingStatusFilter($event)">
                <option value="ALL">All statuses</option><option>PENDING</option><option>CONFIRMED</option><option>REJECTED</option><option>CANCELLED</option>
              </select>
            </label>
          </div>
          @if (bookingsLoading()) { <p class="form-hint" role="status">Loading this booking page…</p> }
          <div class="ops-table-wrap">
            <table class="ops-table">
              <thead><tr>
                <th>Reference</th><th><button type="button" (click)="sortBookings('customerName')">Customer {{ sortMark(bookingsSort(), bookingsDirection(), 'customerName') }}</button></th>
                <th>Journey</th><th>Stay dates</th><th><button type="button" (click)="sortBookings('bookingDate')">Booked {{ sortMark(bookingsSort(), bookingsDirection(), 'bookingDate') }}</button></th>
                <th>Status</th><th>Details</th>
              </tr></thead>
              <tbody>
                @for (booking of filteredBookings(); track booking.id) {
                  <tr>
                    <td><span class="ops-table__primary">{{ booking.id }}</span><span class="ops-table__secondary">{{ booking.bookingType }}</span></td>
                    <td><span class="ops-table__primary">{{ booking.customerName }}</span><span class="ops-table__secondary">{{ booking.email }} · {{ booking.phone }}</span></td>
                    <td>{{ booking.guests }} guest(s)<span class="ops-table__secondary">{{ booking.tripId ? 'Trip ' + booking.tripId : booking.hotelId ? 'Stay ' + booking.hotelId : '—' }}</span></td>
                    <td>{{ booking.startDate | date:'mediumDate' }}{{ booking.endDate ? ' – ' + (booking.endDate | date:'mediumDate') : '' }}</td>
                    <td>{{ booking.bookingDate | date:'mediumDate' }}</td>
                    <td><select [ngModel]="booking.status" [ngModelOptions]="{standalone:true}"
                        [attr.aria-label]="'Status for booking ' + booking.id"
                        [disabled]="updatingBookings().has(booking.id)"
                        (ngModelChange)="setBookingStatus(booking, $event)">
                      @for (status of bookingStatuses; track status) { <option [value]="status">{{ status }}</option> }
                    </select></td>
                    <td><button class="text-button" type="button" (click)="toggleBooking(booking.id)">
                      {{ expandedBookingId() === booking.id ? 'Hide' : 'View' }}
                    </button></td>
                  </tr>
                  @if (expandedBookingId() === booking.id) {
                    <tr><td colspan="7"><div class="ops-details">Customer account: {{ booking.customerId }}
                      @if (booking.tripId) { · Trip ID: {{ booking.tripId }} } @if (booking.hotelId) { · Stay ID: {{ booking.hotelId }} }
                    </div></td></tr>
                  }
                } @empty {
                  <tr><td class="ops-empty" colspan="7">{{ bookingsLoading() ? 'Loading bookings…' : 'No bookings match the loaded page and filters.' }}</td></tr>
                }
              </tbody>
            </table>
          </div>
          <div class="ops-pager">
            <label>Rows per page
              <select name="bookingPageSize" [ngModel]="bookingsSize()" (ngModelChange)="setBookingPageSize($event)">
                @for (size of pageSizes; track size) { <option [ngValue]="size">{{ size }}</option> }
              </select>
            </label>
            <div class="ops-pager__actions"><span>Page {{ bookingsPage() + 1 }} of {{ pageCount(bookingsTotalPages()) }} · {{ bookingsTotal() }} total</span>
              <button class="button button--small" type="button" (click)="changeBookingPage(-1)" [disabled]="bookingsPage() === 0 || bookingsLoading()">Previous</button>
              <button class="button button--small" type="button" (click)="changeBookingPage(1)" [disabled]="bookingsPage() + 1 >= bookingsTotalPages() || bookingsLoading()">Next</button>
            </div>
          </div>
        </section>
      }

      @if (section() !== 'BOOKINGS') {
        <section class="user-list-panel">
          <div class="list-heading ops-table-tools">
            <div><p class="eyebrow">Customer messages</p><h2>Enquiries & Contact Us <span class="admin-count">{{ enquiriesTotal() }}</span></h2></div>
            <label>Status on this page
              <select name="enquiryStatus" [ngModel]="enquiryStatus()" (ngModelChange)="setEnquiryStatusFilter($event)">
                <option value="ALL">All statuses</option><option>OPEN</option><option>IN_PROGRESS</option><option>RESOLVED</option><option>CLOSED</option>
              </select>
            </label>
          </div>
          @if (enquiriesLoading()) { <p class="form-hint" role="status">Loading this enquiry page…</p> }
          <div class="ops-table-wrap">
            <table class="ops-table">
              <thead><tr>
                <th><button type="button" (click)="sortEnquiries('createdAt')">Received {{ sortMark(enquiriesSort(), enquiriesDirection(), 'createdAt') }}</button></th>
                <th><button type="button" (click)="sortEnquiries('name')">Name {{ sortMark(enquiriesSort(), enquiriesDirection(), 'name') }}</button></th>
                <th><button type="button" (click)="sortEnquiries('subject')">Subject {{ sortMark(enquiriesSort(), enquiriesDirection(), 'subject') }}</button></th>
                <th>Contact</th><th>Status</th><th>Message</th>
              </tr></thead>
              <tbody>
                @for (enquiry of filteredEnquiries(); track enquiry.id) {
                  <tr>
                    <td>{{ enquiry.createdAt | date:'medium' }}</td>
                    <td><span class="ops-table__primary">{{ enquiry.name }}</span></td>
                    <td>{{ enquiry.subject }}</td>
                    <td><a [href]="'mailto:' + enquiry.email">{{ enquiry.email }}</a><span class="ops-table__secondary"><a [href]="'tel:' + enquiry.phone">{{ enquiry.phone }}</a></span></td>
                    <td><select [ngModel]="enquiry.status" [ngModelOptions]="{standalone:true}"
                        [attr.aria-label]="'Status for enquiry ' + enquiry.id"
                        [disabled]="updatingEnquiries().has(enquiry.id)"
                        (ngModelChange)="setEnquiryStatus(enquiry, $event)">
                      @for (status of enquiryStatuses; track status) { <option [value]="status">{{ status.replace('_', ' ') }}</option> }
                    </select></td>
                    <td><button class="text-button" type="button" (click)="toggleEnquiry(enquiry.id)">
                      {{ expandedEnquiryId() === enquiry.id ? 'Hide' : 'View message' }}
                    </button></td>
                  </tr>
                  @if (expandedEnquiryId() === enquiry.id) {
                    <tr><td colspan="6"><div class="ops-details">{{ enquiry.message }}</div></td></tr>
                  }
                } @empty {
                  <tr><td class="ops-empty" colspan="6">{{ enquiriesLoading() ? 'Loading enquiries…' : 'No enquiries match the loaded page and filters.' }}</td></tr>
                }
              </tbody>
            </table>
          </div>
          <div class="ops-pager">
            <label>Rows per page
              <select name="enquiryPageSize" [ngModel]="enquiriesSize()" (ngModelChange)="setEnquiryPageSize($event)">
                @for (size of pageSizes; track size) { <option [ngValue]="size">{{ size }}</option> }
              </select>
            </label>
            <div class="ops-pager__actions"><span>Page {{ enquiriesPage() + 1 }} of {{ pageCount(enquiriesTotalPages()) }} · {{ enquiriesTotal() }} total</span>
              <button class="button button--small" type="button" (click)="changeEnquiryPage(-1)" [disabled]="enquiriesPage() === 0 || enquiriesLoading()">Previous</button>
              <button class="button button--small" type="button" (click)="changeEnquiryPage(1)" [disabled]="enquiriesPage() + 1 >= enquiriesTotalPages() || enquiriesLoading()">Next</button>
            </div>
          </div>
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
  readonly bookingsLoading = signal(false);
  readonly enquiriesLoading = signal(false);
  readonly error = signal('');
  readonly search = signal('');
  readonly section = signal<DashboardSection>('ALL');
  readonly bookingStatus = signal<'ALL' | BookingStatus>('ALL');
  readonly enquiryStatus = signal<'ALL' | EnquiryStatus>('ALL');
  readonly expandedBookingId = signal<string | null>(null);
  readonly expandedEnquiryId = signal<string | null>(null);
  readonly bookingStatuses: BookingStatus[] = ['PENDING', 'CONFIRMED', 'REJECTED', 'CANCELLED'];
  readonly enquiryStatuses: EnquiryStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
  readonly pageSizes = [10, 25, 50];
  readonly bookingsPage = signal(0);
  readonly bookingsSize = signal(50);
  readonly bookingsTotal = signal(0);
  readonly bookingsTotalPages = signal(0);
  readonly bookingsSort = signal<BookingSort>('createdAt');
  readonly bookingsDirection = signal<'ASC' | 'DESC'>('DESC');
  readonly enquiriesPage = signal(0);
  readonly enquiriesSize = signal(50);
  readonly enquiriesTotal = signal(0);
  readonly enquiriesTotalPages = signal(0);
  readonly enquiriesSort = signal<EnquirySort>('createdAt');
  readonly enquiriesDirection = signal<'ASC' | 'DESC'>('DESC');
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
  readonly updatingBookings = signal<Set<string>>(new Set());
  readonly updatingEnquiries = signal<Set<string>>(new Set());

  constructor() {
    this.refresh();
  }

  isLoading(): boolean {
    return this.bookingsLoading() || this.enquiriesLoading();
  }

  pageCount(totalPages: number): number {
    return Math.max(totalPages, 1);
  }

  sortMark<T extends string>(current: T, direction: 'ASC' | 'DESC', field: T): string {
    return current === field ? (direction === 'ASC' ? '↑' : '↓') : '↕';
  }

  refresh(): void {
    this.error.set('');
    if (this.section() !== 'ENQUIRIES') this.loadBookings();
    if (this.section() !== 'BOOKINGS') this.loadEnquiries();
  }

  setSection(value: string): void {
    this.section.set(value === 'BOOKINGS' || value === 'ENQUIRIES' ? value : 'ALL');
    this.refresh();
  }

  setBookingStatusFilter(value: string): void {
    if (value === 'ALL' || isBookingStatus(value)) this.bookingStatus.set(value);
  }

  setEnquiryStatusFilter(value: string): void {
    if (value === 'ALL' || isEnquiryStatus(value)) this.enquiryStatus.set(value);
  }

  setBookingPageSize(size: number): void {
    if (!this.pageSizes.includes(Number(size))) return;
    this.bookingsSize.set(Number(size));
    this.bookingsPage.set(0);
    this.loadBookings();
  }

  setEnquiryPageSize(size: number): void {
    if (!this.pageSizes.includes(Number(size))) return;
    this.enquiriesSize.set(Number(size));
    this.enquiriesPage.set(0);
    this.loadEnquiries();
  }

  changeBookingPage(delta: number): void {
    const page = this.bookingsPage() + delta;
    if (page < 0 || page >= this.bookingsTotalPages()) return;
    this.bookingsPage.set(page);
    this.loadBookings();
  }

  changeEnquiryPage(delta: number): void {
    const page = this.enquiriesPage() + delta;
    if (page < 0 || page >= this.enquiriesTotalPages()) return;
    this.enquiriesPage.set(page);
    this.loadEnquiries();
  }

  sortBookings(field: BookingSort): void {
    this.bookingsDirection.set(this.bookingsSort() === field && this.bookingsDirection() === 'ASC' ? 'DESC' : 'ASC');
    this.bookingsSort.set(field);
    this.bookingsPage.set(0);
    this.loadBookings();
  }

  sortEnquiries(field: EnquirySort): void {
    this.enquiriesDirection.set(this.enquiriesSort() === field && this.enquiriesDirection() === 'ASC' ? 'DESC' : 'ASC');
    this.enquiriesSort.set(field);
    this.enquiriesPage.set(0);
    this.loadEnquiries();
  }

  private loadBookings(): void {
    this.bookingsLoading.set(true);
    this.error.set('');
    this.bookingService.getBookingsPage(
      this.bookingsPage(), this.bookingsSize(), this.bookingsSort(), this.bookingsDirection(),
    ).subscribe({
      next: (response) => {
        this.bookings.set(response.content);
        this.bookingsTotal.set(response.totalElements);
        this.bookingsTotalPages.set(response.totalPages);
        this.bookingsLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.bookingsLoading.set(false);
        this.error.set(error.status
          ? `Could not load bookings (${error.status}).`
          : 'Could not reach the bookings API.');
      },
    });
  }

  private loadEnquiries(): void {
    this.enquiriesLoading.set(true);
    this.error.set('');
    this.enquiryService.listPage(
      this.enquiriesPage(), this.enquiriesSize(), this.enquiriesSort(), this.enquiriesDirection(),
    ).subscribe({
      next: (response) => {
        this.enquiries.set(response.content);
        this.enquiriesTotal.set(response.totalElements);
        this.enquiriesTotalPages.set(response.totalPages);
        this.enquiriesLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.enquiriesLoading.set(false);
        this.error.set(error.status
          ? `Could not load enquiries (${error.status}).`
          : 'Could not reach the enquiries API.');
      },
    });
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
