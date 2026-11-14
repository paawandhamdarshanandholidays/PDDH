import { HttpErrorResponse, HttpHandlerFn, HttpRequest, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { describe, expect, it } from 'vitest';
import { AuthService } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  it('adds the bearer token to authenticated requests', () => {
    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: { token: 'test-token' } }],
    });
    const request = new HttpRequest('GET', '/api/private');
    let forwardedRequest: HttpRequest<unknown> | undefined;
    const next: HttpHandlerFn = (forwarded) => {
      forwardedRequest = forwarded;
      return of(new HttpResponse({ url: forwarded.url }));
    };

    TestBed.runInInjectionContext(() => authInterceptor(request, next));

    expect(forwardedRequest?.headers.get('Authorization')).toBe('Bearer test-token');
  });

  it('does not add a token when the user is signed out', () => {
    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: { token: null } }],
    });
    const request = new HttpRequest('GET', '/api/public');
    let forwardedRequest: HttpRequest<unknown> | undefined;
    const next: HttpHandlerFn = (forwarded) => {
      forwardedRequest = forwarded;
      return of(new HttpResponse({ url: forwarded.url }));
    };

    TestBed.runInInjectionContext(() => authInterceptor(request, next));

    expect(forwardedRequest).toBe(request);
  });

  it('does not add a token to login requests', () => {
    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: { token: 'test-token' } }],
    });
    const request = new HttpRequest('POST', '/api/auth/login', null);
    let forwardedRequest: HttpRequest<unknown> | undefined;
    const next: HttpHandlerFn = (forwarded) => {
      forwardedRequest = forwarded;
      return of(new HttpResponse({ url: forwarded.url }));
    };

    TestBed.runInInjectionContext(() => authInterceptor(request, next));

    expect(forwardedRequest).toBe(request);
  });

  it('does not attach a stale token to public registration and recovery requests', () => {
    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: { token: 'expired-token' } }],
    });
    const endpoints = [
      '/auth/register',
      '/auth/register-ceo',
      '/auth/username-availability?email=customer%40example.com&role=CUSTOMER',
      '/auth/password-reset/otp/request',
      '/auth/password-reset/otp/confirm?source=register',
    ];

    for (const endpoint of endpoints) {
      const request = new HttpRequest('POST', `https://api.example.test${endpoint}`, null);
      let forwardedRequest: HttpRequest<unknown> | undefined;
      const next: HttpHandlerFn = (forwarded) => {
        forwardedRequest = forwarded;
        return of(new HttpResponse({ url: forwarded.url }));
      };

      TestBed.runInInjectionContext(() => authInterceptor(request, next));

      expect(forwardedRequest).toBe(request);
    }
  });

  it('clears the session after an authenticated request receives a 401', () => {
    const logout = vi.fn();
    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: { token: 'expired-token', logout } }],
    });
    const request = new HttpRequest('GET', '/api/private');

    TestBed.runInInjectionContext(() => authInterceptor(
      request,
      () => throwError(() => new HttpErrorResponse({ status: 401 })),
    )).subscribe({ error: () => undefined });

    expect(logout).toHaveBeenCalledOnce();
  });
});