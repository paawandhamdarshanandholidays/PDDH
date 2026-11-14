import { Injectable, afterNextRender, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, defer, map, takeUntil, tap, throwError } from 'rxjs';
import { Trip, TripDraft } from '../models/trip.model';
import { getApiBaseUrl } from './api-url';
import { AuthService } from './auth.service';
import { IdempotencySubmission } from './idempotency-key';
import { isPublicHttpUrl } from './image-url.service';

@Injectable({ providedIn: 'root' })
export class TripService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly apiUrl = getApiBaseUrl();
  private readonly submission = new IdempotencySubmission();
  private readonly tripState = signal<Trip[]>([]);
  readonly trips = this.tripState.asReadonly();
  readonly featuredTrips = computed(() => this.tripState().filter((trip) => trip.featured));
  readonly loading = signal(true);
  readonly loadError = signal('');

  constructor() {
    afterNextRender(() => this.loadTrips());
    this.auth.logoutEvents.subscribe(() => {
      this.submission.clear();
      this.tripState.set([]);
      this.loading.set(false);
    });
    this.auth.loginEvents.subscribe(() => this.loadTrips());
  }

  addTrip(draft: TripDraft): Observable<Trip> {
    if (!isPublicHttpUrl(draft.imageUrl)) {
      return throwError(() => new Error('Image values must be public HTTP or HTTPS URLs.'));
    }
    return defer(() => this.http.post<unknown>(`${this.apiUrl}/api/catalog/trips`, draft, {
      headers: new HttpHeaders({ 'Idempotency-Key': this.submission.forPayload(draft) }),
    })).pipe(
      map((value) => {
        if (!isTrip(value)) throw new Error('The saved journey response has an invalid format.');
        return value;
      }),
      tap((trip) => {
        this.tripState.set([trip, ...this.tripState()]);
        this.submission.clear();
      }),
      takeUntil(this.auth.logoutEvents),
    );
  }

  updateTrip(id: string, draft: TripDraft): Observable<Trip> {
    if (!this.tripState().some((trip) => trip.id === id)) {
      return throwError(() => new Error(`Trip "${id}" was not found.`));
    }
    if (!isPublicHttpUrl(draft.imageUrl)) {
      return throwError(() => new Error('Image values must be public HTTP or HTTPS URLs.'));
    }
    return this.http.put<unknown>(`${this.apiUrl}/api/catalog/trips/${encodeURIComponent(id)}`, draft).pipe(
      map((value) => {
        if (!isTrip(value)) throw new Error('The updated journey response has an invalid format.');
        return value;
      }),
      tap((trip) => this.tripState.set(this.tripState().map((item) => item.id === id ? trip : item))),
      takeUntil(this.auth.logoutEvents),
    );
  }

  deleteTrip(id: string): Observable<void> {
    if (!this.tripState().some((trip) => trip.id === id)) {
      return throwError(() => new Error(`Trip "${id}" was not found.`));
    }
    return this.http.delete<void>(`${this.apiUrl}/api/catalog/trips/${encodeURIComponent(id)}`).pipe(
      tap(() => this.tripState.set(this.tripState().filter((trip) => trip.id !== id))),
      takeUntil(this.auth.logoutEvents),
    );
  }

  private loadTrips(): void {
    this.loading.set(true);
    this.loadError.set('');
    this.http.get<unknown>(`${this.apiUrl}/api/catalog/trips`).pipe(
      takeUntil(this.auth.logoutEvents),
    ).subscribe({
      next: (value) => {
        this.loading.set(false);
        if (Array.isArray(value) && value.every(isTrip)) {
          this.tripState.set(value);
        } else {
          this.loadError.set('The trips API returned data in an invalid format.');
          console.error(this.loadError());
        }
      },
      error: (error) => {
        this.loading.set(false);
        this.loadError.set('Could not load journeys from the trips database API.');
        console.error(this.loadError(), error);
      },
    });
  }
}

function isTrip(value: unknown): value is Trip {
  if (!value || typeof value !== 'object') return false;
  const trip = value as Record<string, unknown>;
  return typeof trip['id'] === 'string'
    && typeof trip['title'] === 'string'
    && typeof trip['destination'] === 'string'
    && typeof trip['summary'] === 'string'
    && typeof trip['imageUrl'] === 'string'
    && typeof trip['startDate'] === 'string'
    && typeof trip['durationDays'] === 'number'
    && typeof trip['price'] === 'number'
    && (trip['category'] === 'Pilgrimage' || trip['category'] === 'Heritage' || trip['category'] === 'Nature')
    && typeof trip['featured'] === 'boolean';
}
