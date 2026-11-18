import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { BookingService } from '../../core/services/booking.service';
import { EnquiryService, EnquiryStatus } from '../../core/services/enquiry.service';
import { ManagementDashboard } from './management-dashboard';

describe('ManagementDashboard', () => {
  let fixture: ComponentFixture<ManagementDashboard>;
  let updateBookingStatus: ReturnType<typeof vi.fn>;
  let updateBooking: ReturnType<typeof vi.fn>;
  let deleteBooking: ReturnType<typeof vi.fn>;
  let updateEnquiryStatus: ReturnType<typeof vi.fn>;
  let updateEnquiry: ReturnType<typeof vi.fn>;
  let deleteEnquiry: ReturnType<typeof vi.fn>;
  let getBookingsPage: ReturnType<typeof vi.fn>;
  let listEnquiryPage: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    getBookingsPage = vi.fn(() => of({
      content: [{
        id: 'booking-1',
        customerId: 'asha',
        bookingDate: '2026-11-14',
        bookingType: 'TRIP',
        customerName: 'Asha',
        email: 'asha@example.test',
        phone: '123',
        guests: 2,
        startDate: '2026-12-01',
        status: 'PENDING',
        tripId: 'trip-1',
      }],
      page: 0,
      size: 50,
      totalElements: 51,
      totalPages: 2,
    }));
    listEnquiryPage = vi.fn(() => of({
      content: [{
        id: 'enquiry-1',
        name: 'Asha',
        email: 'asha@example.test',
        phone: '123',
        subject: 'Travel question',
        message: 'Please share trip details.',
        status: 'OPEN',
        createdAt: '2026-11-14T00:00:00Z',
      }],
      page: 0,
      size: 50,
      totalElements: 51,
      totalPages: 2,
    }));
    const booking = {
      id: 'booking-1',
      customerId: 'asha',
      bookingDate: '2026-11-14',
      bookingType: 'TRIP' as const,
      customerName: 'Asha',
      email: 'asha@example.test',
      phone: '123',
      guests: 2,
      startDate: '2026-12-01',
      endDate: '2026-12-05',
      tripId: 'trip-1',
      status: 'PENDING' as const,
    };
    updateBookingStatus = vi.fn((_id: string, status: 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'CANCELLED') =>
      of({ ...booking, status }));
    updateBooking = vi.fn((id: string, update: typeof booking) => of({ ...booking, ...update, id }));
    deleteBooking = vi.fn(() => of(void 0));
    updateEnquiryStatus = vi.fn((id: string, status: EnquiryStatus) => of({
      id,
      name: 'Asha',
      email: 'asha@example.test',
      phone: '123',
      subject: 'Travel question',
      message: 'Please share trip details.',
      status,
      createdAt: '2026-11-14T00:00:00Z',
    }));
    updateEnquiry = vi.fn((id: string, update: Record<string, string>) => of({
      id,
      ...update,
      status: 'OPEN' as const,
      createdAt: '2026-11-14T00:00:00Z',
    }));
    deleteEnquiry = vi.fn(() => of(void 0));
    await TestBed.configureTestingModule({
      imports: [ManagementDashboard],
      providers: [
        {
          provide: BookingService,
          useValue: {
            getBookingsPage,
            updateStatus: updateBookingStatus,
            update: updateBooking,
            delete: deleteBooking,
          },
        },
        {
          provide: EnquiryService,
          useValue: {
            listPage: listEnquiryPage,
            updateStatus: updateEnquiryStatus,
            update: updateEnquiry,
            delete: deleteEnquiry,
          },
        },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ManagementDashboard);
    fixture.detectChanges();
  });

  it('loads bookings and enquiries only after their summary buttons are clicked', () => {
    expect(getBookingsPage).not.toHaveBeenCalled();
    expect(listEnquiryPage).not.toHaveBeenCalled();
    const buttons = fixture.nativeElement.querySelectorAll('.ops-summary button');
    expect(buttons).toHaveLength(2);
    expect(fixture.nativeElement.textContent).not.toContain('Asha');

    buttons[0].click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Asha');
    expect(getBookingsPage).toHaveBeenCalledWith(0, 50, 'createdAt', 'DESC', '');
    expect(listEnquiryPage).not.toHaveBeenCalled();

    buttons[1].click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Travel question');
    expect(listEnquiryPage).toHaveBeenCalledWith(0, 50, 'createdAt', 'DESC', '');
  });

  it('updates booking and enquiry statuses using their existing APIs', () => {
    const component = fixture.componentInstance;
    component.setSection('BOOKINGS');
    component.setSection('ENQUIRIES');
    component.setBookingStatus(component.bookings()[0]!, 'CONFIRMED');
    component.setEnquiryStatus(component.enquiries()[0]!, 'IN_PROGRESS');

    expect(updateBookingStatus).toHaveBeenCalledWith('booking-1', 'CONFIRMED');
    expect(updateEnquiryStatus).toHaveBeenCalledWith('enquiry-1', 'IN_PROGRESS');
    expect(component.bookings()[0]?.status).toBe('CONFIRMED');
    expect(component.enquiries()[0]?.status).toBe('IN_PROGRESS');
  });

  it('updates and deletes bookings and enquiries without changing their IDs', () => {
    vi.stubGlobal('confirm', () => true);
    const component = fixture.componentInstance;
    component.setSection('BOOKINGS');
    const booking = component.bookings()[0]!;
    component.editBooking(booking);
    component.bookingDraft.customerName = 'Updated customer';
    component.saveBooking(booking);
    expect(updateBooking).toHaveBeenCalledWith('booking-1', expect.objectContaining({
      customerName: 'Updated customer',
    }));
    expect(component.bookings()[0]?.customerName).toBe('Updated customer');
    component.deleteBooking(component.bookings()[0]!);
    expect(deleteBooking).toHaveBeenCalledWith('booking-1');

    component.setSection('ENQUIRIES');
    const enquiry = component.enquiries()[0]!;
    component.editEnquiry(enquiry);
    component.enquiryDraft.subject = 'Updated subject';
    component.saveEnquiry(enquiry);
    expect(updateEnquiry).toHaveBeenCalledWith('enquiry-1', expect.objectContaining({
      subject: 'Updated subject',
    }));
    expect(component.enquiries()[0]?.subject).toBe('Updated subject');
    component.deleteEnquiry(component.enquiries()[0]!);
    expect(deleteEnquiry).toHaveBeenCalledWith('enquiry-1');
    vi.unstubAllGlobals();
  });

  it('searches database results and pages them using the configured size', () => {
    const component = fixture.componentInstance;
    component.setSection('BOOKINGS');
    component.search.set('asha@example.test');
    component.searchDatabase();
    expect(getBookingsPage).toHaveBeenLastCalledWith(0, 50, 'createdAt', 'DESC', 'asha@example.test');

    component.setBookingPageSize(10000);
    expect(getBookingsPage).toHaveBeenLastCalledWith(0, 10000, 'createdAt', 'DESC', 'asha@example.test');
    component.setBookingPageSize(10001);
    expect(getBookingsPage).toHaveBeenCalledTimes(3);

    component.setSection('ENQUIRIES');
    component.changeEnquiryPage(1);
    expect(listEnquiryPage).toHaveBeenLastCalledWith(1, 50, 'createdAt', 'DESC', 'asha@example.test');
  });
});
