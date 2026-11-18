import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { BookingRecord, BookingStatus, BookingUpdate } from '../../core/models/booking.model';
import { BookingService } from '../../core/services/booking.service';
import { EnquiryRecord, EnquiryService, EnquiryStatus, EnquiryUpdate } from '../../core/services/enquiry.service';

type DashboardSection = 'BOOKINGS' | 'ENQUIRIES' | null;
type BookingSort = 'createdAt' | 'customerName' | 'bookingDate' | 'status';
type EnquirySort = 'createdAt' | 'name' | 'subject' | 'status';
type BookingEditDraft = BookingUpdate & { endDate: string; tripId: string; hotelId: string };
type EnquiryEditDraft = EnquiryUpdate;

const emptyBookingDraft = (): BookingEditDraft => ({
  customerName: '', email: '', phone: '', guests: 1, startDate: '', endDate: '', tripId: '', hotelId: '',
});

@Component({
  selector: 'app-management-dashboard',
  standalone: true,
  imports: [DatePipe, FormsModule],
  styles: [`
    .ops-summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 1rem; margin: 1.5rem 0; }
    .ops-summary button { display: grid; grid-template-columns: 1fr auto; align-items: center; gap: .35rem 1rem; min-height: 116px; padding: 1.25rem 1.5rem; border: 1px solid var(--line); border-radius: 1.1rem; background: linear-gradient(135deg, #fff, #f5f8fc); text-align: left; cursor: pointer; transition: border-color .18s ease, box-shadow .18s ease, transform .18s ease; }
    .ops-summary button:hover, .ops-summary button[aria-pressed="true"] { border-color: #b98548; box-shadow: 0 12px 30px rgb(39 52 68 / 10%); transform: translateY(-2px); }
    .ops-summary button:focus-visible { outline: 3px solid rgb(185 133 72 / 35%); outline-offset: 3px; }
    .ops-summary strong { display: block; color: var(--ink); font-size: 1.8rem; }
    .ops-summary span { color: var(--muted); font-size: .85rem; }
    .ops-summary__copy { display: grid; gap: .15rem; }
    .ops-summary__eyebrow { color: #a56d31 !important; font-size: .68rem !important; font-weight: 700; letter-spacing: .13em; text-transform: uppercase; }
    .ops-summary__arrow { grid-column: 2; grid-row: 1 / span 3; color: #a56d31; font-size: 1.35rem; }
    .ops-toolbar, .ops-table-tools, .ops-pager { display: flex; align-items: end; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
    .ops-toolbar { margin: 1.25rem 0; }
    .ops-toolbar label, .ops-table-tools label { min-width: 190px; }
    .ops-search { display: flex; align-items: end; gap: .6rem; width: min(100%, 680px); }
    .ops-search label { flex: 1; }
    .ops-search input { width: 100%; }
    .ops-welcome { padding: 2.25rem 1.5rem; border: 1px dashed var(--line); border-radius: 1rem; color: var(--muted); text-align: center; }
    .ops-table-wrap { overflow-x: auto; border: 1px solid var(--line); border-radius: 1rem; background: #fff; }
    .ops-table { width: 100%; min-width: 920px; border-collapse: collapse; }
    .ops-table th, .ops-table td { padding: .85rem 1rem; border-bottom: 1px solid var(--line); text-align: left; vertical-align: top; }
    .ops-table th { color: var(--muted); background: #f8fafc; font-size: .72rem; letter-spacing: .06em; text-transform: uppercase; }
    .ops-table th button { padding: 0; border: 0; background: none; color: inherit; font: inherit; letter-spacing: inherit; text-transform: inherit; cursor: pointer; }
    .ops-table td { color: var(--ink); font-size: .86rem; }
    .ops-table tbody tr:hover { background: #f8fafc; }
    .ops-table__primary { font-weight: 700; }
    .ops-table__secondary { display: block; margin-top: .2rem; color: var(--muted); font-size: .76rem; }
    .ops-table__actions { display: flex; align-items: center; flex-wrap: wrap; gap: .75rem; }
    .ops-pager { padding: .9rem 0; color: var(--muted); font-size: .85rem; }
    .ops-pager__actions { display: flex; align-items: center; gap: .6rem; }
    .ops-details { max-width: 52rem; white-space: pre-wrap; overflow-wrap: anywhere; }
    .ops-empty { padding: 2rem; text-align: center; color: var(--muted); }
  `],
  template: `
    <section class="admin-page">
      <div class="admin-heading">
        <div><p class="eyebrow">Operations workspace</p><h1>Bookings, enquiries & contact</h1>
          <p>Choose a workspace to load records from your database. Search across all matching requests.</p>
        </div>
        @if (section()) { <button class="text-button" type="button" (click)="refresh()" [disabled]="isLoading()">Refresh</button> }
      </div>
      @if (error()) { <div class="notice notice--error" role="alert">{{ error() }}</div> }
      @if (message()) { <div class="notice notice--success" role="status">{{ message() }}</div> }

      <div class="ops-summary">
        <button type="button" [attr.aria-pressed]="section() === 'BOOKINGS'" (click)="setSection('BOOKINGS')">
          <span class="ops-summary__copy"><span class="ops-summary__eyebrow">Travel requests</span>
            <strong>{{ bookingsLoaded() ? bookingsTotal() : '—' }}</strong><span>Bookings</span></span>
          <span class="ops-summary__arrow" aria-hidden="true">→</span>
        </button>
        <button type="button" [attr.aria-pressed]="section() === 'ENQUIRIES'" (click)="setSection('ENQUIRIES')">
          <span class="ops-summary__copy"><span class="ops-summary__eyebrow">Customer messages</span>
            <strong>{{ enquiriesLoaded() ? enquiriesTotal() : '—' }}</strong><span>Enquiries & Contact Us</span></span>
          <span class="ops-summary__arrow" aria-hidden="true">→</span>
        </button>
      </div>

      @if (section()) {
        <form class="ops-toolbar" (ngSubmit)="searchDatabase()">
          <div class="ops-search">
            <label>Search all {{ section() === 'BOOKINGS' ? 'bookings' : 'enquiries' }}
              <input type="search" name="search" [ngModel]="search()" (ngModelChange)="search.set($event)"
                placeholder="Name, email, phone, reference, status…">
            </label>
            <button class="button button--small" type="submit" [disabled]="isLoading()">Search</button>
            @if (appliedSearch()) {
              <button class="text-button" type="button" (click)="clearSearch()" [disabled]="isLoading()">Clear</button>
            }
          </div>
        </form>
      } @else {
        <div class="ops-welcome">Select <strong>Bookings</strong> or <strong>Enquiries & Contact Us</strong> above to load records.</div>
      }

      @if (section() === 'BOOKINGS') {
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
                <th>Status</th><th>Actions</th>
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
                    <td>
                      <div class="ops-table__actions">
                        <button class="text-button" type="button" (click)="toggleBooking(booking.id)">
                          {{ expandedBookingId() === booking.id ? 'Hide' : 'View' }}
                        </button>
                        <button class="text-button" type="button" (click)="editBooking(booking)"
                          [disabled]="updatingBookings().has(booking.id)">Edit</button>
                        <button class="text-button text-button--danger" type="button" (click)="deleteBooking(booking)"
                          [disabled]="updatingBookings().has(booking.id)">Delete</button>
                      </div>
                    </td>
                  </tr>
                  @if (editingBookingId() === booking.id) {
                    <tr><td colspan="7">
                      <form class="form-card" (ngSubmit)="saveBooking(booking)" #bookingEditForm="ngForm">
                        <p class="eyebrow">Edit booking</p><h3>{{ booking.id }} <span class="form-hint">Reference cannot be changed</span></h3>
                        <div class="form-row">
                          <label>Customer name<input name="bookingCustomerName" [(ngModel)]="bookingDraft.customerName" required maxlength="120"></label>
                          <label>Email<input name="bookingEmail" [(ngModel)]="bookingDraft.email" required type="email" maxlength="254"></label>
                          <label>Phone<input name="bookingPhone" [(ngModel)]="bookingDraft.phone" required maxlength="30" type="tel"></label>
                          <label>Guests<input name="bookingGuests" [(ngModel)]="bookingDraft.guests" required type="number" min="1" max="30"></label>
                        </div>
                        <div class="form-row">
                          <label>Start date<input name="bookingStartDate" [(ngModel)]="bookingDraft.startDate" required type="date"></label>
                          @if (booking.bookingType === 'TRIP') {
                            <label>End date<input name="bookingEndDate" [(ngModel)]="bookingDraft.endDate" required type="date"></label>
                            <label>Trip ID<input name="bookingTripId" [(ngModel)]="bookingDraft.tripId" required maxlength="120"></label>
                          } @else {
                            <label>End date<input name="bookingEndDate" [(ngModel)]="bookingDraft.endDate" type="date"></label>
                            <label>Accommodation ID<input name="bookingHotelId" [(ngModel)]="bookingDraft.hotelId" required maxlength="120"></label>
                          }
                        </div>
                        <div class="user-card__actions">
                          <button class="button button--small" type="submit"
                            [disabled]="bookingEditForm.invalid || updatingBookings().has(booking.id)">Save changes</button>
                          <button class="text-button" type="button" (click)="editingBookingId.set(null)">Cancel</button>
                        </div>
                      </form>
                    </td></tr>
                  }
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

      @if (section() === 'ENQUIRIES') {
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
                <th>Contact</th><th>Status</th><th>Actions</th>
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
                    <td>
                      <div class="ops-table__actions">
                        <button class="text-button" type="button" (click)="toggleEnquiry(enquiry.id)">
                          {{ expandedEnquiryId() === enquiry.id ? 'Hide' : 'View message' }}
                        </button>
                        <button class="text-button" type="button" (click)="editEnquiry(enquiry)"
                          [disabled]="updatingEnquiries().has(enquiry.id)">Edit</button>
                        <button class="text-button text-button--danger" type="button" (click)="deleteEnquiry(enquiry)"
                          [disabled]="updatingEnquiries().has(enquiry.id)">Delete</button>
                      </div>
                    </td>
                  </tr>
                  @if (editingEnquiryId() === enquiry.id) {
                    <tr><td colspan="6">
                      <form class="form-card" (ngSubmit)="saveEnquiry(enquiry)" #enquiryEditForm="ngForm">
                        <p class="eyebrow">Edit enquiry</p><h3>{{ enquiry.id }} <span class="form-hint">Reference and received date cannot be changed</span></h3>
                        <div class="form-row">
                          <label>Name<input name="enquiryName" [(ngModel)]="enquiryDraft.name" required maxlength="120"></label>
                          <label>Email<input name="enquiryEmail" [(ngModel)]="enquiryDraft.email" required type="email" maxlength="254"></label>
                          <label>Phone<input name="enquiryPhone" [(ngModel)]="enquiryDraft.phone" required maxlength="30" type="tel"></label>
                          <label>Subject<input name="enquirySubject" [(ngModel)]="enquiryDraft.subject" required maxlength="160"></label>
                        </div>
                        <label>Message<textarea name="enquiryMessage" [(ngModel)]="enquiryDraft.message" required maxlength="5000" rows="5"></textarea></label>
                        <div class="user-card__actions">
                          <button class="button button--small" type="submit"
                            [disabled]="enquiryEditForm.invalid || updatingEnquiries().has(enquiry.id)">Save changes</button>
                          <button class="text-button" type="button" (click)="editingEnquiryId.set(null)">Cancel</button>
                        </div>
                      </form>
                    </td></tr>
                  }
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
  readonly message = signal('');
  readonly search = signal('');
  readonly appliedSearch = signal('');
  readonly section = signal<DashboardSection>(null);
  readonly bookingStatus = signal<'ALL' | BookingStatus>('ALL');
  readonly enquiryStatus = signal<'ALL' | EnquiryStatus>('ALL');
  readonly expandedBookingId = signal<string | null>(null);
  readonly expandedEnquiryId = signal<string | null>(null);
  readonly editingBookingId = signal<string | null>(null);
  readonly editingEnquiryId = signal<string | null>(null);
  bookingDraft = emptyBookingDraft();
  enquiryDraft: EnquiryEditDraft = { name: '', email: '', phone: '', subject: '', message: '' };
  readonly bookingStatuses: BookingStatus[] = ['PENDING', 'CONFIRMED', 'REJECTED', 'CANCELLED'];
  readonly enquiryStatuses: EnquiryStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
  readonly pageSizes = [25, 50, 100, 500, 1000, 5000, 10000];
  readonly bookingsLoaded = signal(false);
  readonly bookingsPage = signal(0);
  readonly bookingsSize = signal(50);
  readonly bookingsTotal = signal(0);
  readonly bookingsTotalPages = signal(0);
  readonly bookingsSort = signal<BookingSort>('createdAt');
  readonly bookingsDirection = signal<'ASC' | 'DESC'>('DESC');
  readonly enquiriesPage = signal(0);
  readonly enquiriesSize = signal(50);
  readonly enquiriesLoaded = signal(false);
  readonly enquiriesTotal = signal(0);
  readonly enquiriesTotalPages = signal(0);
  readonly enquiriesSort = signal<EnquirySort>('createdAt');
  readonly enquiriesDirection = signal<'ASC' | 'DESC'>('DESC');
  readonly filteredBookings = computed(() => {
    return this.bookings().filter((booking) =>
      this.bookingStatus() === 'ALL' || booking.status === this.bookingStatus());
  });
  readonly filteredEnquiries = computed(() => {
    return this.enquiries().filter((enquiry) =>
      this.enquiryStatus() === 'ALL' || enquiry.status === this.enquiryStatus());
  });
  readonly updatingBookings = signal<Set<string>>(new Set());
  readonly updatingEnquiries = signal<Set<string>>(new Set());

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
    if (this.section() === 'BOOKINGS') this.loadBookings();
    if (this.section() === 'ENQUIRIES') this.loadEnquiries();
  }

  setSection(value: string): void {
    if (value !== 'BOOKINGS' && value !== 'ENQUIRIES') return;
    this.section.set(value);
    this.error.set('');
    if (value === 'BOOKINGS') this.loadBookings();
    if (value === 'ENQUIRIES') this.loadEnquiries();
  }

  searchDatabase(): void {
    if (!this.section()) return;
    this.appliedSearch.set(this.search().trim());
    if (this.section() === 'BOOKINGS') {
      this.bookingsPage.set(0);
      this.loadBookings();
    } else {
      this.enquiriesPage.set(0);
      this.loadEnquiries();
    }
  }

  clearSearch(): void {
    this.search.set('');
    this.appliedSearch.set('');
    this.searchDatabase();
  }

  setBookingStatusFilter(value: string): void {
    if (value === 'ALL' || isBookingStatus(value)) this.bookingStatus.set(value);
  }

  setEnquiryStatusFilter(value: string): void {
    if (value === 'ALL' || isEnquiryStatus(value)) this.enquiryStatus.set(value);
  }

  setBookingPageSize(size: number): void {
    if (!Number.isInteger(Number(size)) || Number(size) < 1 || Number(size) > 10000) return;
    this.bookingsSize.set(Number(size));
    this.bookingsPage.set(0);
    this.loadBookings();
  }

  setEnquiryPageSize(size: number): void {
    if (!Number.isInteger(Number(size)) || Number(size) < 1 || Number(size) > 10000) return;
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
      this.bookingsPage(), this.bookingsSize(), this.bookingsSort(), this.bookingsDirection(), this.appliedSearch(),
    ).subscribe({
      next: (response) => {
        this.bookings.set(response.content);
        this.bookingsTotal.set(response.totalElements);
        this.bookingsTotalPages.set(response.totalPages);
        this.bookingsLoaded.set(true);
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
      this.enquiriesPage(), this.enquiriesSize(), this.enquiriesSort(), this.enquiriesDirection(), this.appliedSearch(),
    ).subscribe({
      next: (response) => {
        this.enquiries.set(response.content);
        this.enquiriesTotal.set(response.totalElements);
        this.enquiriesTotalPages.set(response.totalPages);
        this.enquiriesLoaded.set(true);
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
      next: (updated) => this.bookings.update((items) =>
        items.map((item) => item.id === booking.id ? updated : item)),
      error: (error: HttpErrorResponse) => this.error.set(error.status
        ? `Could not update booking ${booking.id} (${error.status}).`
        : 'Could not reach the bookings API to update this record.'),
    });
  }

  editBooking(booking: BookingRecord): void {
    this.error.set('');
    this.message.set('');
    this.bookingDraft = {
      customerName: booking.customerName,
      email: booking.email,
      phone: booking.phone,
      guests: booking.guests,
      startDate: booking.startDate,
      endDate: booking.endDate ?? '',
      tripId: booking.tripId ?? '',
      hotelId: booking.hotelId ?? '',
    };
    this.editingBookingId.set(booking.id);
  }

  saveBooking(booking: BookingRecord): void {
    if (this.updatingBookings().has(booking.id)) return;
    const update: BookingUpdate = {
      ...this.bookingDraft,
      endDate: this.bookingDraft.endDate || undefined,
      tripId: this.bookingDraft.tripId || undefined,
      hotelId: this.bookingDraft.hotelId || undefined,
    };
    this.error.set('');
    this.message.set('');
    this.updatingBookings.update((current) => new Set(current).add(booking.id));
    this.bookingService.update(booking.id, update).pipe(finalize(() =>
      this.updatingBookings.update((current) => {
        const next = new Set(current);
        next.delete(booking.id);
        return next;
      }))).subscribe({
      next: (updated) => {
        this.bookings.update((items) => items.map((item) => item.id === booking.id ? updated : item));
        this.editingBookingId.set(null);
        this.message.set(`Booking ${booking.id} updated.`);
      },
      error: (error: HttpErrorResponse) => this.error.set(error.status
        ? `Could not update booking ${booking.id} (${error.status}).`
        : 'Could not reach the bookings API to update this record.'),
    });
  }

  deleteBooking(booking: BookingRecord): void {
    if (!globalThis.confirm(`Permanently delete booking ${booking.id}?`)
        || this.updatingBookings().has(booking.id)) return;
    this.error.set('');
    this.message.set('');
    this.updatingBookings.update((current) => new Set(current).add(booking.id));
    this.bookingService.delete(booking.id).pipe(finalize(() =>
      this.updatingBookings.update((current) => {
        const next = new Set(current);
        next.delete(booking.id);
        return next;
      }))).subscribe({
      next: () => {
        this.bookings.update((items) => items.filter((item) => item.id !== booking.id));
        this.bookingsTotal.update((total) => Math.max(0, total - 1));
        this.editingBookingId.set(null);
        this.message.set(`Booking ${booking.id} deleted.`);
        if (this.bookings().length === 0 && this.bookingsPage() > 0) {
          this.bookingsPage.update((page) => page - 1);
          this.loadBookings();
        }
      },
      error: (error: HttpErrorResponse) => this.error.set(error.status
        ? `Could not delete booking ${booking.id} (${error.status}).`
        : 'Could not reach the bookings API to delete this record.'),
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

  editEnquiry(enquiry: EnquiryRecord): void {
    this.error.set('');
    this.message.set('');
    this.enquiryDraft = {
      name: enquiry.name,
      email: enquiry.email,
      phone: enquiry.phone,
      subject: enquiry.subject,
      message: enquiry.message,
    };
    this.editingEnquiryId.set(enquiry.id);
  }

  saveEnquiry(enquiry: EnquiryRecord): void {
    if (this.updatingEnquiries().has(enquiry.id)) return;
    this.error.set('');
    this.message.set('');
    this.updatingEnquiries.update((current) => new Set(current).add(enquiry.id));
    this.enquiryService.update(enquiry.id, this.enquiryDraft).pipe(finalize(() =>
      this.updatingEnquiries.update((current) => {
        const next = new Set(current);
        next.delete(enquiry.id);
        return next;
      }))).subscribe({
      next: (updated) => {
        this.enquiries.update((items) => items.map((item) => item.id === enquiry.id ? updated : item));
        this.editingEnquiryId.set(null);
        this.message.set(`Enquiry ${enquiry.id} updated.`);
      },
      error: (error: HttpErrorResponse) => this.error.set(error.status
        ? `Could not update enquiry ${enquiry.id} (${error.status}).`
        : 'Could not reach the enquiries API to update this record.'),
    });
  }

  deleteEnquiry(enquiry: EnquiryRecord): void {
    if (!globalThis.confirm(`Permanently delete enquiry ${enquiry.id}?`)
        || this.updatingEnquiries().has(enquiry.id)) return;
    this.error.set('');
    this.message.set('');
    this.updatingEnquiries.update((current) => new Set(current).add(enquiry.id));
    this.enquiryService.delete(enquiry.id).pipe(finalize(() =>
      this.updatingEnquiries.update((current) => {
        const next = new Set(current);
        next.delete(enquiry.id);
        return next;
      }))).subscribe({
      next: () => {
        this.enquiries.update((items) => items.filter((item) => item.id !== enquiry.id));
        this.enquiriesTotal.update((total) => Math.max(0, total - 1));
        this.editingEnquiryId.set(null);
        this.message.set(`Enquiry ${enquiry.id} deleted.`);
        if (this.enquiries().length === 0 && this.enquiriesPage() > 0) {
          this.enquiriesPage.update((page) => page - 1);
          this.loadEnquiries();
        }
      },
      error: (error: HttpErrorResponse) => this.error.set(error.status
        ? `Could not delete enquiry ${enquiry.id} (${error.status}).`
        : 'Could not reach the enquiries API to delete this record.'),
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
