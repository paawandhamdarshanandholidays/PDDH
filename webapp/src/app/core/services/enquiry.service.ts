import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { getApiBaseUrl } from './api-url';
import { createIdempotencyKey } from './idempotency-key';

export interface EnquiryRequest {
  name: string;
  email: string;
  phone: string;
  subject: string;
  message: string;
}

export interface EnquiryResponse {
  id: string;
  status: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class EnquiryService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = getApiBaseUrl();

  submit(request: EnquiryRequest, idempotencyKey = createIdempotencyKey()): Observable<EnquiryResponse> {
    return this.http.post<EnquiryResponse>(`${this.apiUrl}/api/enquiries`, request, {
      headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey }),
    });
  }
}
