import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { BookingService } from '../../core/services/booking.service';
import { CustomerBookings } from './customer-bookings';

describe('CustomerBookings', () => {
  let fixture: ComponentFixture<CustomerBookings>;
  let getBookingsPage: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    getBookingsPage = vi.fn((page: number, size: number) => of({
      content: [],
      page,
      size,
      totalElements: 51,
      totalPages: 2,
    }));
    await TestBed.configureTestingModule({
      imports: [CustomerBookings],
      providers: [{ provide: BookingService, useValue: { getBookingsPage } }],
    }).compileComponents();
    fixture = TestBed.createComponent(CustomerBookings);
    fixture.detectChanges();
  });

  it('loads one page of at most 50 customer bookings at a time', () => {
    expect(getBookingsPage).toHaveBeenCalledWith(0, 50, 'createdAt', 'DESC');

    fixture.componentInstance.changePage(1);

    expect(getBookingsPage).toHaveBeenLastCalledWith(1, 50, 'createdAt', 'DESC');
  });
});
