import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiPage } from '../models/api-page.model';
import { getApiBaseUrl } from './api-url';

export interface UserDirectoryEntry {
  id: number;
  name: string;
  contactNumber: string;
  email: string;
  address: string;
  referredBy: string;
  serviceProvided: string;
}

export type UserDirectoryDraft = Omit<UserDirectoryEntry, 'id'>;

@Injectable({ providedIn: 'root' })
export class UserDirectoryService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = getApiBaseUrl();

  list(): Observable<UserDirectoryEntry[]> {
    return this.http.get<UserDirectoryEntry[]>(`${this.apiUrl}/api/users/directory`);
  }

  listPage(
    page: number,
    size: number,
    sort: string,
    direction: 'ASC' | 'DESC',
  ): Observable<ApiPage<UserDirectoryEntry>> {
    return this.http.get<ApiPage<UserDirectoryEntry>>(`${this.apiUrl}/api/users/directory/page`, {
      params: { page, size, sort, direction },
    });
  }

  create(entry: UserDirectoryDraft): Observable<UserDirectoryEntry> {
    return this.http.post<UserDirectoryEntry>(`${this.apiUrl}/api/users/directory`, entry);
  }
}
