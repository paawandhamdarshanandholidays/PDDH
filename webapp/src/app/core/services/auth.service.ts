import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, OnDestroy, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Observable, Subject, tap } from 'rxjs';
import { getApiBaseUrl } from './api-url';
import { UserRole } from '../models/role.model';

export interface LoginRequest {
  username: string;
  password: string;
  role: UserRole;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken?: string;
}

export interface RegistrationRequest {
  name: string;
  username: string;
  password: string;
  role?: UserRole;
  email: string;
  mobile?: string;
  gender?: string;
}

export interface RegistrationResponse {
  message: string;
}

export interface UsernameAvailabilityResponse {
  username: string;
  usernameAvailable: boolean;
  emailAvailable: boolean;
  mobileAvailable: boolean;
}

export interface PasswordResetOtpRequest {
  email: string;
}

export interface PasswordResetOtpConfirmRequest {
  email: string;
  code: string;
  newPassword: string;
}

const TOKEN_KEY = 'pddh.accessToken';
const ROLE_KEY = 'pddh.accountRole';
const LOGOUT_CHANNEL = 'pddh.auth';
const CONTENT_CACHE_KEYS = [
  'pddh.temples.v1',
  'pddh.places.v1',
  'pddh.events.v1',
  'pddh.gallery.v1',
  'pddh.hotels.v1',
  'pddh.offers.v1',
  'pddh.trips.v1',
];
const USER_ROLES: readonly UserRole[] = ['CEO', 'ADMIN', 'MANAGER', 'STAFF', 'CUSTOMER'];

type AuthBroadcastMessage =
  | { type: 'login'; accessToken: string; role: UserRole }
  | { type: 'logout' };

@Injectable({ providedIn: 'root' })
export class AuthService implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly tokenState = signal(this.readToken());
  private readonly roleState = signal<UserRole | null>(this.readRole());
  private readonly loginState = new Subject<void>();
  readonly loginEvents = this.loginState.asObservable();
  private readonly logoutState = new Subject<void>();
  readonly logoutEvents = this.logoutState.asObservable();
  private readonly logoutChannel = this.createLogoutChannel();
  readonly isAuthenticated = () => Boolean(this.tokenState());
  readonly isCeo = () => this.roleState() === 'CEO';
  readonly role = () => this.roleState();
  private readonly apiUrl = getApiBaseUrl();

  constructor() {
    this.logoutChannel?.addEventListener('message', this.handleCrossTabLogout);
    this.clearLegacyPersistentAuth();
    this.clearLegacyContentCache();
    if (isPlatformBrowser(this.platformId)) {
      window.addEventListener('pagehide', this.handlePageHide);
    }
  }

  ngOnDestroy(): void {
    this.logoutChannel?.removeEventListener('message', this.handleCrossTabLogout);
    this.logoutChannel?.close();
    if (isPlatformBrowser(this.platformId)) {
      window.removeEventListener('pagehide', this.handlePageHide);
    }
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/auth/login`, credentials).pipe(
      tap((response) => {
        if (!response.accessToken) {
          throw new Error('The login response did not include an access token.');
        }
        if (isPlatformBrowser(this.platformId)) {
          sessionStorage.setItem(TOKEN_KEY, response.accessToken);
          sessionStorage.setItem(ROLE_KEY, credentials.role);
          this.clearLegacyPersistentAuth();
          this.logoutChannel?.postMessage({
            type: 'login',
            accessToken: response.accessToken,
            role: credentials.role,
          } satisfies AuthBroadcastMessage);
        }
        this.tokenState.set(response.accessToken);
        this.roleState.set(credentials.role);
        this.loginState.next();
      }),
    );
  }

  availableRoles(): Observable<UserRole[]> {
    return this.http.get<UserRole[]>(`${this.apiUrl}/auth/roles`);
  }

  register(request: RegistrationRequest): Observable<RegistrationResponse> {
    return this.http.post<RegistrationResponse>(`${this.apiUrl}/auth/register`, request);
  }

  registerCeo(request: RegistrationRequest): Observable<RegistrationResponse> {
    return this.http.post<RegistrationResponse>(`${this.apiUrl}/auth/register-ceo`, request);
  }

  checkUsernameAvailability(
    email: string,
    role: UserRole,
    username?: string,
    mobile?: string,
  ): Observable<UsernameAvailabilityResponse> {
    let params = new HttpParams().set('email', email).set('role', role);
    if (username) params = params.set('username', username);
    if (mobile) params = params.set('mobile', mobile);
    return this.http.get<UsernameAvailabilityResponse>(
      `${this.apiUrl}/auth/username-availability`,
      { params },
    );
  }

  requestPasswordResetOtp(request: PasswordResetOtpRequest): Observable<RegistrationResponse> {
    return this.http.post<RegistrationResponse>(`${this.apiUrl}/auth/password-reset/otp/request`, request);
  }

  confirmPasswordResetOtp(request: PasswordResetOtpConfirmRequest): Observable<RegistrationResponse> {
    return this.http.post<RegistrationResponse>(`${this.apiUrl}/auth/password-reset/otp/confirm`, request);
  }

  logout(): void {
    if (isPlatformBrowser(this.platformId)) {
      sessionStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(ROLE_KEY);
      this.clearLegacyPersistentAuth();
      this.clearLegacyContentCache();
    }
    this.tokenState.set(null);
    this.roleState.set(null);
    this.logoutState.next();
    this.logoutChannel?.postMessage({ type: 'logout' } satisfies AuthBroadcastMessage);
  }

  get token(): string | null {
    return this.tokenState();
  }

  private readToken(): string | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    try {
      return sessionStorage.getItem(TOKEN_KEY);
    } catch (error) {
      console.error('Could not read the saved sign-in session.', error);
      return null;
    }
  }

  private readRole(): UserRole | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    try {
      const role = sessionStorage.getItem(ROLE_KEY);
      return role && USER_ROLES.includes(role as UserRole) ? role as UserRole : null;
    } catch (error) {
      console.error('Could not read the saved account role.', error);
      return null;
    }
  }

  private readonly handleCrossTabLogout = (event: MessageEvent): void => {
    const message = event.data as AuthBroadcastMessage;
    if (!message || typeof message !== 'object') return;
    if (message.type === 'logout') {
      sessionStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(ROLE_KEY);
      this.tokenState.set(null);
      this.roleState.set(null);
      this.clearLegacyContentCache();
      this.logoutState.next();
    } else if (message.type === 'login'
      && typeof message.accessToken === 'string'
      && USER_ROLES.includes(message.role)) {
      sessionStorage.setItem(TOKEN_KEY, message.accessToken);
      sessionStorage.setItem(ROLE_KEY, message.role);
      this.tokenState.set(message.accessToken);
      this.roleState.set(message.role);
      this.loginState.next();
    }
  };

  private readonly handlePageHide = (): void => this.logout();

  private createLogoutChannel(): BroadcastChannel | null {
    if (!isPlatformBrowser(this.platformId) || typeof BroadcastChannel === 'undefined') return null;
    return new BroadcastChannel(LOGOUT_CHANNEL);
  }

  private clearLegacyPersistentAuth(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    try {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(ROLE_KEY);
    } catch (error) {
      console.error('Could not clear the legacy persistent sign-in session.', error);
    }
  }

  private clearLegacyContentCache(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    try {
      for (const key of CONTENT_CACHE_KEYS) localStorage.removeItem(key);
    } catch (error) {
      console.error('Could not clear the legacy website content cache.', error);
    }
  }
}
