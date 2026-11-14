import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { BookingService } from '../../core/services/booking.service';
import { EnquiryService, EnquiryStatus } from '../../core/services/enquiry.service';
import { ManagementDashboard } from './management-dashboard';

describe('ManagementDashboard', () => {
  let fixture: ComponentFixture<ManagementDashboard>;
  let updateBookingStatus: ReturnType<typeof vi.fn>;
  let updateEnquiryStatus: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
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
            getBookings: () => of([{
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
            }]),
            updateStatus: updateBookingStatus,
          },
        },
        {
          provide: EnquiryService,
          useValue: {
            list: () => of([{
              id: 'enquiry-1',
              name: 'Asha',
              email: 'asha@example.test',
              phone: '123',
              subject: 'Travel question',
              message: 'Please share trip details.',
              status: 'OPEN',
              createdAt: '2026-11-14T00:00:00Z',
            }]),
            updateStatus: updateEnquiryStatus,
          },
        },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ManagementDashboard);
    fixture.detectChanges();
  });

  it('shows booking and contact enquiry records together', () => {
    expect(fixture.nativeElement.textContent).toContain('Asha');
    expect(fixture.nativeElement.textContent).toContain('Travel question');
    expect(fixture.nativeElement.textContent).toContain('Bookings');
    expect(fixture.nativeElement.textContent).toContain('Enquiries & Contact Us');
  });

  it('updates booking and enquiry statuses using their existing APIs', () => {
    const component = fixture.componentInstance;
    component.setBookingStatus(component.bookings()[0]!, 'CONFIRMED');
    component.setEnquiryStatus(component.enquiries()[0]!, 'IN_PROGRESS');

    expect(updateBookingStatus).toHaveBeenCalledWith('booking-1', 'CONFIRMED');
    expect(updateEnquiryStatus).toHaveBeenCalledWith('enquiry-1', 'IN_PROGRESS');
  });
});
