import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { getApiBaseUrl } from './api-url';

@Injectable({ providedIn: 'root' })
export class ImageUrlService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = getApiBaseUrl();

  resolveUnsplashPhotoPage(url: string): Observable<{ imageUrl: string }> {
    return this.http.get<{ imageUrl: string }>(`${this.apiUrl}/api/catalog/temples/resolve-image`, {
      params: new HttpParams().set('url', url),
    });
  }
}

export function isPublicHttpUrl(value: string): boolean {
  try {
    const url = new URL(value.trim());
    return (url.protocol === 'http:' || url.protocol === 'https:')
      && Boolean(url.hostname)
      && !url.username
      && !url.password;
  } catch {
    return false;
  }
}
