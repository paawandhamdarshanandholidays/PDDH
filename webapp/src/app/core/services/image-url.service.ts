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

export function isImageSource(value: string): boolean {
  return isPublicHttpUrl(value) || value.startsWith('data:image/');
}

export function resolveCatalogImageUrl(value: string, apiUrl = getApiBaseUrl()): string {
  if (!value.startsWith('/api/catalog/')) return value;
  try {
    const resolved = new URL(value, apiUrl);
    if (resolved.origin === new URL(apiUrl).origin
        && /^\/api\/catalog\/[a-z-]+\/[0-9a-f-]+\/image$/i.test(resolved.pathname)) {
      return resolved.toString();
    }
  } catch {
    return value;
  }
  return value;
}
