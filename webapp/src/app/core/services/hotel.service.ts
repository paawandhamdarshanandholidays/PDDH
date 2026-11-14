import { Injectable, afterNextRender, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, defer, map, takeUntil, tap, throwError } from 'rxjs';
import { Hotel, HotelDraft } from '../models/hotel.model';
import { getApiBaseUrl } from './api-url';
import { AuthService } from './auth.service';
import { IdempotencySubmission } from './idempotency-key';
import { isPublicHttpUrl } from './image-url.service';

@Injectable({ providedIn: 'root' })
export class HotelService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly apiUrl = getApiBaseUrl();
  private readonly submission = new IdempotencySubmission();
  private readonly hotelState = signal<Hotel[]>([]);
  readonly hotels = this.hotelState.asReadonly();

  constructor() {
    afterNextRender(() => this.loadHotels());
    this.auth.logoutEvents.subscribe(() => {
      this.submission.clear();
      this.hotelState.set([]);
    });
    this.auth.loginEvents.subscribe(() => this.loadHotels());
  }

  addHotel(draft: HotelDraft): Observable<Hotel> {
    if (!isPublicHttpUrl(draft.imageUrl)) {
      return throwError(() => new Error('Image values must be public HTTP or HTTPS URLs.'));
    }
    return defer(() => this.http.post<unknown>(`${this.apiUrl}/api/catalog/hotels`, draft, {
      headers: new HttpHeaders({ 'Idempotency-Key': this.submission.forPayload(draft) }),
    })).pipe(
      map((value) => {
        if (!isHotel(value)) throw new Error('The saved accommodation response has an invalid format.');
        return value;
      }),
      tap((hotel) => {
        this.hotelState.set([hotel, ...this.hotelState()]);
        this.submission.clear();
      }),
      takeUntil(this.auth.logoutEvents),
    );
  }

  removeHotel(id: string): Observable<void> {
    if (!this.hotelState().some((hotel) => hotel.id === id)) return throwError(() => new Error('Accommodation listing was not found.'));
    return this.http.delete<void>(`${this.apiUrl}/api/catalog/hotels/${encodeURIComponent(id)}`).pipe(
      tap(() => this.hotelState.set(this.hotelState().filter((hotel) => hotel.id !== id))),
      takeUntil(this.auth.logoutEvents),
    );
  }

  private loadHotels(): void {
    this.http.get<unknown>(`${this.apiUrl}/api/catalog/hotels`).pipe(
      takeUntil(this.auth.logoutEvents),
    ).subscribe({
      next: (value) => {
        if (Array.isArray(value) && value.every(isHotel)) {
          this.hotelState.set(value);
        } else {
          console.error('Could not load accommodation: the catalog response has an invalid format.');
        }
      },
      error: (error) => console.error('Could not load accommodation from the catalog API.', error),
    });
  }
}

function isHotel(value: unknown): value is Hotel {
  if (!value || typeof value !== 'object') return false;
  const hotel = value as Record<string, unknown>;
  return typeof hotel['id'] === 'string'
    && typeof hotel['name'] === 'string'
    && typeof hotel['location'] === 'string'
    && typeof hotel['description'] === 'string'
    && typeof hotel['imageUrl'] === 'string'
    && typeof hotel['pricePerNight'] === 'number'
    && typeof hotel['amenities'] === 'string';
}
