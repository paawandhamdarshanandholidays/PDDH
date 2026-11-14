import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, afterNextRender, inject, signal } from '@angular/core';
import { Observable, defer, map, takeUntil, tap, throwError } from 'rxjs';
import { getApiBaseUrl } from './api-url';
import { AuthService } from './auth.service';
import { IdempotencySubmission } from './idempotency-key';

export interface ContactDetails {
  id: string;
  phone: string;
  email: string;
  officeHours: string;
}

export type ContactDetailsDraft = Omit<ContactDetails, 'id'>;

@Injectable({ providedIn: 'root' })
export class ContactDetailsService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly apiUrl = getApiBaseUrl();
  private readonly submission = new IdempotencySubmission();
  private readonly itemsState = signal<ContactDetails[]>([]);
  readonly items = this.itemsState.asReadonly();

  constructor() {
    afterNextRender(() => this.load());
    this.auth.logoutEvents.subscribe(() => {
      this.submission.clear();
      this.itemsState.set([]);
    });
    this.auth.loginEvents.subscribe(() => this.load());
  }

  add(draft: ContactDetailsDraft): Observable<ContactDetails> {
    return defer(() => this.http.post<unknown>(`${this.apiUrl}/api/catalog/contact`, draft, {
      headers: new HttpHeaders({ 'Idempotency-Key': this.submission.forPayload(draft) }),
    })).pipe(
      map((value) => {
        if (!isContactDetails(value)) throw new Error('The saved contact details response has an invalid format.');
        return value;
      }),
      tap((item) => {
        this.itemsState.set([item, ...this.itemsState()]);
        this.submission.clear();
      }),
      takeUntil(this.auth.logoutEvents),
    );
  }

  update(id: string, draft: ContactDetailsDraft): Observable<ContactDetails> {
    if (!this.itemsState().some((item) => item.id === id)) {
      return throwError(() => new Error(`Contact details "${id}" were not found.`));
    }
    return this.http.put<unknown>(`${this.apiUrl}/api/catalog/contact/${encodeURIComponent(id)}`, draft).pipe(
      map((value) => {
        if (!isContactDetails(value)) throw new Error('The updated contact details response has an invalid format.');
        return value;
      }),
      tap((item) => this.itemsState.set(this.itemsState().map((current) => current.id === id ? item : current))),
      takeUntil(this.auth.logoutEvents),
    );
  }

  remove(id: string): Observable<void> {
    if (!this.itemsState().some((item) => item.id === id)) {
      return throwError(() => new Error(`Contact details "${id}" were not found.`));
    }
    return this.http.delete<void>(`${this.apiUrl}/api/catalog/contact/${encodeURIComponent(id)}`).pipe(
      tap(() => this.itemsState.set(this.itemsState().filter((item) => item.id !== id))),
      takeUntil(this.auth.logoutEvents),
    );
  }

  private load(): void {
    this.http.get<unknown>(`${this.apiUrl}/api/catalog/contact`).pipe(
      takeUntil(this.auth.logoutEvents),
    ).subscribe({
      next: (value) => {
        if (Array.isArray(value) && value.every(isContactDetails)) {
          this.itemsState.set(value);
        } else {
          console.error('Could not load contact details: the catalog response has an invalid format.');
        }
      },
      error: (error) => console.error('Could not load contact details from the catalog API.', error),
    });
  }
}

function isContactDetails(value: unknown): value is ContactDetails {
  if (!value || typeof value !== 'object') return false;
  const details = value as Record<string, unknown>;
  return typeof details['id'] === 'string'
    && typeof details['phone'] === 'string'
    && typeof details['email'] === 'string'
    && typeof details['officeHours'] === 'string';
}
