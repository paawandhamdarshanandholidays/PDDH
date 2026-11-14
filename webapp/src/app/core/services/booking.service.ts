import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { BookingRecord, BookingRequest, BookingResponse, BookingStatus } from '../models/booking.model';
import { getApiBaseUrl } from './api-url';
import { createIdempotencyKey } from './idempotency-key';

@Injectable({ providedIn: 'root' })
export class BookingService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = getApiBaseUrl();

  submit(request: BookingRequest, idempotencyKey = createIdempotencyKey()): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(`${this.apiUrl}/api/bookings`, request, {
      headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey }),
    });
  }

  getBookings(): Observable<BookingRecord[]> {
    return this.http.get<BookingRecord[]>(`${this.apiUrl}/api/bookings`);
  }

  updateStatus(id: string, status: BookingStatus): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/api/bookings/${encodeURIComponent(id)}/status`, { status });
  }
}
