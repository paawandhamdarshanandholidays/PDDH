import { Injectable, afterNextRender, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, defer, map, takeUntil, tap, throwError } from 'rxjs';
import { GalleryMedia } from '../models/content.model';
import { getApiBaseUrl } from './api-url';
import { AuthService } from './auth.service';
import { IdempotencySubmission } from './idempotency-key';
import { isImageSource, isPublicHttpUrl, resolveCatalogImageUrl } from './image-url.service';

@Injectable({ providedIn: 'root' })
export class GalleryService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly apiUrl = getApiBaseUrl();
  private readonly submission = new IdempotencySubmission();
  private readonly imageState = signal<GalleryMedia[]>([]);
  readonly images = this.imageState.asReadonly();
  readonly categories = computed(() => ['All', ...new Set(this.imageState().map((image) => image.category))]);

  constructor() {
    afterNextRender(() => this.loadGallery());
    this.auth.logoutEvents.subscribe(() => {
      this.submission.clear();
      this.imageState.set([]);
    });
    this.auth.loginEvents.subscribe(() => this.loadGallery());
  }

  addMedia(draft: Omit<GalleryMedia, 'id'>): Observable<GalleryMedia> {
    const validSource = draft.mediaType === 'VIDEO'
      ? isPublicHttpUrl(draft.src)
      : isImageSource(draft.src);
    if (!validSource) {
      return throwError(() => new Error(
        draft.mediaType === 'VIDEO'
          ? 'Video values must be public HTTP or HTTPS URLs.'
          : 'Image values must be public HTTP or HTTPS URLs or supported image files.',
      ));
    }
    return defer(() => this.http.post<unknown>(`${this.apiUrl}/api/catalog/gallery`, draft, {
      headers: new HttpHeaders({ 'Idempotency-Key': this.submission.forPayload(draft) }),
    })).pipe(
      map((value) => {
        if (!isGalleryMedia(value)) throw new Error('The saved gallery media response has an invalid format.');
        return { ...value, src: resolveCatalogImageUrl(value.src, this.apiUrl) };
      }),
      tap((image) => {
        this.imageState.set([image, ...this.imageState()]);
        this.submission.clear();
      }),
      takeUntil(this.auth.logoutEvents),
    );
  }

  removeImage(id: string): Observable<void> {
    if (!this.imageState().some((image) => image.id === id)) return throwError(() => new Error('Gallery image was not found.'));
    return this.http.delete<void>(`${this.apiUrl}/api/catalog/gallery/${encodeURIComponent(id)}`).pipe(
      tap(() => this.imageState.set(this.imageState().filter((image) => image.id !== id))),
      takeUntil(this.auth.logoutEvents),
    );
  }

  private loadGallery(): void {
    this.http.get<unknown>(`${this.apiUrl}/api/catalog/gallery`).pipe(
      takeUntil(this.auth.logoutEvents),
    ).subscribe({
      next: (value) => {
        if (Array.isArray(value) && value.every(isGalleryMedia)) {
          this.imageState.set(value.map((image) => ({
            ...image,
            src: resolveCatalogImageUrl(image.src, this.apiUrl),
          })));
        } else {
          console.error('Could not load gallery images: the catalog response has an invalid format.');
        }
      },
      error: (error) => console.error('Could not load gallery images from the catalog API.', error),
    });
  }
}

function isGalleryMedia(value: unknown): value is GalleryMedia {
  if (!value || typeof value !== 'object') return false;
  const media = value as Record<string, unknown>;
  return typeof media['id'] === 'string'
    && typeof media['src'] === 'string'
    && typeof media['alt'] === 'string'
    && typeof media['category'] === 'string'
    && (media['mediaType'] === undefined || media['mediaType'] === 'IMAGE' || media['mediaType'] === 'VIDEO');
}
