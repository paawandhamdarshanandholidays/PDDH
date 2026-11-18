import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { BookingService } from '../../core/services/booking.service';
import { EnquiryService, EnquiryStatus } from '../../core/services/enquiry.service';
import { ManagementDashboard } from './management-dashboard';

describe('ManagementDashboard', () => {
  let fixture: ComponentFixture<ManagementDashboard>;
  let updateBookingStatus: ReturnType<typeof vi.fn>;
  let updateEnquiryStatus: ReturnType<typeof vi.fn>;
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
    updateBookingStatus = vi.fn(() => of(void 0));
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
    await TestBed.configureTestingModule({
      imports: [ManagementDashboard],
      providers: [
        {
          provide: BookingService,
          useValue: {
            getBookingsPage,
            updateStatus: updateBookingStatus,
          },
        },
        {
          provide: EnquiryService,
          useValue: {
            listPage: listEnquiryPage,
            updateStatus: updateEnquiryStatus,
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
