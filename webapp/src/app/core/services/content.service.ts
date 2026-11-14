import { Injectable, WritableSignal, afterNextRender, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, defer, map, takeUntil, tap, throwError } from 'rxjs';
import { Destination, Event, GalleryImage } from '../models/content.model';
import { getApiBaseUrl } from './api-url';
import { AuthService } from './auth.service';
import { IdempotencySubmission } from './idempotency-key';
import { isImageSource, isPublicHttpUrl, resolveCatalogImageUrl } from './image-url.service';

@Injectable({ providedIn: 'root' })
export class ContentService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly apiUrl = getApiBaseUrl();
  private readonly destinationSubmission = new IdempotencySubmission();
  private readonly eventSubmission = new IdempotencySubmission();
  private readonly templeState = signal<Destination[]>([]);
  private readonly placeState = signal<Destination[]>([]);
  private readonly eventState = signal<Event[]>([]);
  readonly temples = this.templeState.asReadonly();
  readonly places = this.placeState.asReadonly();
  readonly events = this.eventState.asReadonly();

  constructor() {
    afterNextRender(() => {
      this.loadDestinations('temples', 'Temple', this.templeState);
      this.loadDestinations('places', 'Place', this.placeState);
      this.loadEvents();
    });
    this.auth.logoutEvents.subscribe(() => {
      this.destinationSubmission.clear();
      this.eventSubmission.clear();
      this.templeState.set([]);
      this.placeState.set([]);
      this.eventState.set([]);
    });
    this.auth.loginEvents.subscribe(() => {
      this.loadDestinations('temples', 'Temple', this.templeState);
      this.loadDestinations('places', 'Place', this.placeState);
      this.loadEvents();
    });
  }

  addDestination(draft: Omit<Destination, 'id'>): Observable<Destination> {
    if (!isImageSource(draft.imageUrl)) {
      return throwError(() => new Error('Image values must be public HTTP or HTTPS URLs.'));
    }
    const state = draft.kind === 'Temple' ? this.templeState : this.placeState;
    const resource = draft.kind === 'Temple' ? 'temples' : 'places';
    return defer(() => this.http.post<unknown>(`${this.apiUrl}/api/catalog/${resource}`, draft, {
      headers: new HttpHeaders({ 'Idempotency-Key': this.destinationSubmission.forPayload(draft) }),
    })).pipe(
      map((value) => {
        if (!isDestination(value)) throw new Error('The saved destination response has an invalid format.');
        return { ...value, imageUrl: resolveCatalogImageUrl(value.imageUrl, this.apiUrl) };
      }),
      tap((item) => {
        state.set([item, ...state()]);
        this.destinationSubmission.clear();
      }),
      takeUntil(this.auth.logoutEvents),
    );
  }

  removeDestination(id: string, kind: Destination['kind']): Observable<void> {
    const state = kind === 'Temple' ? this.templeState : this.placeState;
    if (!state().some((item) => item.id === id)) return throwError(() => new Error('Destination was not found.'));
    const resource = kind === 'Temple' ? 'temples' : 'places';
    return this.http.delete<void>(`${this.apiUrl}/api/catalog/${resource}/${encodeURIComponent(id)}`).pipe(
      tap(() => state.set(state().filter((item) => item.id !== id))),
      takeUntil(this.auth.logoutEvents),
    );
  }

  addEvent(draft: Omit<Event, 'id'>): Observable<Event> {
    if (!isImageSource(draft.imageUrl)) {
      return throwError(() => new Error('Image values must be public HTTP or HTTPS URLs.'));
    }
    return defer(() => this.http.post<unknown>(`${this.apiUrl}/api/catalog/events`, draft, {
      headers: new HttpHeaders({ 'Idempotency-Key': this.eventSubmission.forPayload(draft) }),
    })).pipe(
      map((value) => {
        if (!isEvent(value)) throw new Error('The saved event response has an invalid format.');
        return { ...value, imageUrl: resolveCatalogImageUrl(value.imageUrl, this.apiUrl) };
      }),
      tap((event) => {
        this.eventState.set([event, ...this.eventState()]);
        this.eventSubmission.clear();
      }),
      takeUntil(this.auth.logoutEvents),
    );
  }

  removeEvent(id: string): Observable<void> {
    if (!this.eventState().some((event) => event.id === id)) return throwError(() => new Error('Event was not found.'));
    return this.http.delete<void>(`${this.apiUrl}/api/catalog/events/${encodeURIComponent(id)}`).pipe(
      tap(() => this.eventState.set(this.eventState().filter((event) => event.id !== id))),
      takeUntil(this.auth.logoutEvents),
    );
  }

  private loadDestinations(
    resource: 'temples' | 'places',
    kind: Destination['kind'],
    state: WritableSignal<Destination[]>,
  ): void {
    this.http.get<unknown>(`${this.apiUrl}/api/catalog/${resource}`).pipe(
      takeUntil(this.auth.logoutEvents),
    ).subscribe({
      next: (value) => {
        if (!Array.isArray(value)) {
          console.error(`Could not load ${resource}: the catalog response has an invalid format.`);
          return;
        }
        const destinations = value.map((item) => {
          if (!item || typeof item !== 'object') return item;
          const record = item as Record<string, unknown>;
          return {
            ...record,
            kind,
            imageUrl: typeof record['imageUrl'] === 'string'
              ? resolveCatalogImageUrl(record['imageUrl'], this.apiUrl)
              : record['imageUrl'],
          } as Destination;
        });
        if (!destinations.every(isDestination)) {
          console.error(`Could not load ${resource}: the catalog response has an invalid format.`);
          return;
        }
        state.set(destinations.filter(isDestination));
      },
      error: (error) => console.error(`Could not load ${resource} from the catalog API.`, error),
    });
  }

  private loadEvents(): void {
    this.http.get<unknown>(`${this.apiUrl}/api/catalog/events`).pipe(
      takeUntil(this.auth.logoutEvents),
    ).subscribe({
      next: (value) => {
        if (Array.isArray(value) && value.every(isEvent)) {
          this.eventState.set(value.map((event) => ({
            ...event,
            imageUrl: resolveCatalogImageUrl(event.imageUrl, this.apiUrl),
          })));
        } else {
          console.error('Could not load events: the catalog response has an invalid format.');
        }
      },
      error: (error) => console.error('Could not load events from the catalog API.', error),
    });
  }

}

function isDestination(value: unknown): value is Destination {
  if (!value || typeof value !== 'object') return false;
  const item = value as Record<string, unknown>;
  return typeof item['id'] === 'string'
    && (item['kind'] === 'Temple' || item['kind'] === 'Place')
    && typeof item['name'] === 'string'
    && typeof item['region'] === 'string'
    && typeof item['description'] === 'string'
    && typeof item['imageUrl'] === 'string'
    && (item['category'] === 'Temple' || item['category'] === 'Nature' || item['category'] === 'Heritage');
}

function isEvent(value: unknown): value is Event {
  if (!value || typeof value !== 'object') return false;
  const item = value as Record<string, unknown>;
  return typeof item['id'] === 'string'
    && typeof item['title'] === 'string'
    && typeof item['date'] === 'string'
    && typeof item['location'] === 'string'
    && typeof item['description'] === 'string'
    && typeof item['imageUrl'] === 'string';
}
