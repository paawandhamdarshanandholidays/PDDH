import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { getApiBaseUrl } from './api-url';

export const MAX_IMAGE_FILE_SIZE_BYTES = 5 * 1024 * 1024;
const MAX_IMAGE_DATA_URL_LENGTH = Math.ceil(MAX_IMAGE_FILE_SIZE_BYTES / 3) * 4 + 64;

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
  return isPublicHttpUrl(value) || isSupportedImageDataUrl(value);
}

export function isSupportedImageDataUrl(value: string): boolean {
  if (value.length > MAX_IMAGE_DATA_URL_LENGTH) return false;
  const match = /^data:image\/(?:png|jpeg|gif|webp);base64,([A-Za-z0-9+/]*={0,2})$/.exec(value);
  if (!match) return false;
  const base64 = match[1];
  if (!base64 || base64.length % 4 !== 0) return false;
  const padding = base64.endsWith('==') ? 2 : base64.endsWith('=') ? 1 : 0;
  const decodedSize = base64.length / 4 * 3 - padding;
  return decodedSize > 0 && decodedSize <= MAX_IMAGE_FILE_SIZE_BYTES;
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
