import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateUserRequest, UserProfile, UserRole } from '../models/role.model';
import { getApiBaseUrl } from './api-url';
import { createIdempotencyKey } from './idempotency-key';

export interface ProfileUpdate {
  name: string;
  email: string;
  mobile: string;
  role?: UserProfile['role'];
}

export interface UserProfilePage {
  content: UserProfile[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PasswordResetRequest {
  targetUserId?: string;
  newPassword: string;
}

export interface PasswordResetApproval {
  id: string;
  targetUserId: string;
  targetName: string;
  targetUsername: string;
  requesterName: string;
  requestedAt: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
}

@Injectable({ providedIn: 'root' })
export class UserManagementService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = getApiBaseUrl();

  listRoles(): Observable<UserRole[]> {
    return this.http.get<UserRole[]>(`${this.apiUrl}/auth/roles`);
  }

  listUsers(role: UserProfile['role'], page: number, size: number): Observable<UserProfilePage> {
    const roleService = role.toLowerCase();
    return this.http.get<UserProfilePage>(`${this.apiUrl}/api/roles/${roleService}/profiles`, {
      params: { page, size },
    });
  }

  createUser(request: CreateUserRequest, idempotencyKey = createIdempotencyKey()): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/api/users`, request, {
      headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey }),
    });
  }

  updateUser(id: string, update: ProfileUpdate): Observable<UserProfile> {
    return this.http.patch<UserProfile>(`${this.apiUrl}/api/users/${encodeURIComponent(id)}`, update);
  }

  setUserActive(id: string, active: boolean): Observable<UserProfile> {
    return this.http.patch<UserProfile>(`${this.apiUrl}/api/users/${encodeURIComponent(id)}/status`, { active });
  }

  deleteUser(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/users/${encodeURIComponent(id)}`);
  }

  getMyProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/api/users/me`);
  }

  updateMyProfile(update: Omit<ProfileUpdate, 'role'>): Observable<UserProfile> {
    return this.http.patch<UserProfile>(`${this.apiUrl}/api/users/me`, update);
  }

  changeMyPassword(currentPassword: string, newPassword: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/api/users/me/password`, {
      currentPassword,
      newPassword,
    });
  }

  requestPasswordReset(request: PasswordResetRequest): Observable<PasswordResetApproval> {
    return this.http.post<PasswordResetApproval>(`${this.apiUrl}/api/users/password-reset-requests`, request);
  }

  listPendingPasswordResets(): Observable<PasswordResetApproval[]> {
    return this.http.get<PasswordResetApproval[]>(`${this.apiUrl}/api/users/password-reset-requests`);
  }

  listPasswordResetTargets(): Observable<UserProfile[]> {
    return this.http.get<UserProfile[]>(`${this.apiUrl}/api/users/password-reset-targets`);
  }

  reviewPasswordReset(id: string, approve: boolean): Observable<PasswordResetApproval> {
    return this.http.patch<PasswordResetApproval>(
      `${this.apiUrl}/api/users/password-reset-requests/${encodeURIComponent(id)}`,
      null,
      { params: { approve } },
    );
  }
}
