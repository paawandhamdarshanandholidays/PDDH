import { Injectable, afterNextRender, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, defer, map, takeUntil, tap, throwError } from 'rxjs';
import { Offer, OfferDraft } from '../models/offer.model';
import { getApiBaseUrl } from './api-url';
import { AuthService } from './auth.service';
import { IdempotencySubmission } from './idempotency-key';
import { isImageSource, resolveCatalogImageUrl } from './image-url.service';

@Injectable({ providedIn: 'root' })
export class OfferService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly apiUrl = getApiBaseUrl();
  private readonly submission = new IdempotencySubmission();
  private readonly offerState = signal<Offer[]>([]);
  readonly offers = this.offerState.asReadonly();
  readonly activeOffers = computed(() => this.offerState().filter((offer) => offer.active && offer.validUntil >= new Date().toISOString().slice(0, 10)));

  constructor() {
    afterNextRender(() => this.loadOffers());
    this.auth.logoutEvents.subscribe(() => {
      this.submission.clear();
      this.offerState.set([]);
    });
    this.auth.loginEvents.subscribe(() => this.loadOffers());
  }

  addOffer(draft: OfferDraft): Observable<Offer> {
    if (draft.imageUrl && !isImageSource(draft.imageUrl)) {
      return throwError(() => new Error('Image values must be public HTTP or HTTPS URLs.'));
    }
    return defer(() => this.http.post<unknown>(`${this.apiUrl}/api/catalog/offers`, draft, {
      headers: new HttpHeaders({ 'Idempotency-Key': this.submission.forPayload(draft) }),
    })).pipe(
      map((value) => {
        if (!isOffer(value)) throw new Error('The saved offer response has an invalid format.');
        return {
          ...value,
          imageUrl: value.imageUrl ? resolveCatalogImageUrl(value.imageUrl, this.apiUrl) : undefined,
        };
      }),
      tap((offer) => {
        this.offerState.set([offer, ...this.offerState()]);
        this.submission.clear();
      }),
      takeUntil(this.auth.logoutEvents),
    );
  }

  removeOffer(id: string): Observable<void> {
    if (!this.offerState().some((offer) => offer.id === id)) return throwError(() => new Error('Offer was not found.'));
    return this.http.delete<void>(`${this.apiUrl}/api/catalog/offers/${encodeURIComponent(id)}`).pipe(
      tap(() => this.offerState.set(this.offerState().filter((offer) => offer.id !== id))),
      takeUntil(this.auth.logoutEvents),
    );
  }

  private loadOffers(): void {
    this.http.get<unknown>(`${this.apiUrl}/api/catalog/offers`).pipe(
      takeUntil(this.auth.logoutEvents),
    ).subscribe({
      next: (value) => {
        if (Array.isArray(value) && value.every(isOffer)) {
          this.offerState.set(value.map((offer) => ({
            ...offer,
            imageUrl: offer.imageUrl ? resolveCatalogImageUrl(offer.imageUrl, this.apiUrl) : undefined,
          })));
        } else {
          console.error('Could not load offers: the catalog response has an invalid format.');
        }
      },
      error: (error) => console.error('Could not load offers from the catalog API.', error),
    });
  }
}

function isOffer(value: unknown): value is Offer {
  if (!value || typeof value !== 'object') return false;
  const offer = value as Record<string, unknown>;
  return typeof offer['id'] === 'string'
    && typeof offer['title'] === 'string'
    && typeof offer['description'] === 'string'
    && typeof offer['code'] === 'string'
    && typeof offer['discountPercent'] === 'number'
    && typeof offer['validUntil'] === 'string'
    && typeof offer['tripId'] === 'string'
    && (offer['imageUrl'] === undefined || typeof offer['imageUrl'] === 'string')
    && typeof offer['active'] === 'boolean';
}
