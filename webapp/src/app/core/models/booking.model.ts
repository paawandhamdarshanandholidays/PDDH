export interface BookingRequest {
  bookingType: 'TRIP' | 'STAY';
  customerName: string;
  phone: string;
  guests: number;
  startDate: string;
  endDate?: string;
  tripId?: string;
  hotelId?: string;
}

export interface BookingResponse {
  reference?: string;
  message?: string;
  bookingDate?: string;
}

export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'CANCELLED';

export interface BookingRecord {
  id: string;
  customerId: string;
  bookingDate: string;
  bookingType: 'TRIP' | 'STAY';
  customerName: string;
  email: string;
  phone: string;
  guests: number;
  startDate: string;
  endDate?: string;
  tripId?: string;
  hotelId?: string;
  status: BookingStatus;
}
