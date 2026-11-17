import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiPage } from '../models/api-page.model';
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
  status: EnquiryStatus;
  createdAt: string;
}

export type EnquiryStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';

export interface EnquiryRecord extends EnquiryResponse {
  name: string;
  email: string;
  phone: string;
  subject: string;
  message: string;
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

  list(): Observable<EnquiryRecord[]> {
    return this.http.get<EnquiryRecord[]>(`${this.apiUrl}/api/enquiries`);
  }

  listPage(
    page: number,
    size: number,
    sort: string,
    direction: 'ASC' | 'DESC',
  ): Observable<ApiPage<EnquiryRecord>> {
    return this.http.get<ApiPage<EnquiryRecord>>(`${this.apiUrl}/api/enquiries/page`, {
      params: { page, size, sort, direction },
    });
  }

  updateStatus(id: string, status: EnquiryStatus): Observable<EnquiryRecord> {
    return this.http.patch<EnquiryRecord>(
      `${this.apiUrl}/api/enquiries/${encodeURIComponent(id)}/status`,
      { status },
    );
  }
}
