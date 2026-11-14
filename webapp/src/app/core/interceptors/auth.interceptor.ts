import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const token = auth.token;
  const path = request.url.split('?')[0];
  const publicAuthEndpoints = [
    '/auth/login',
    '/auth/roles',
    '/auth/register',
    '/auth/register-ceo',
    '/auth/username-availability',
    '/auth/password-reset/otp/request',
    '/auth/password-reset/otp/confirm',
  ];
  if (!token || publicAuthEndpoints.some((endpoint) => path.endsWith(endpoint))) return next(request);
  return next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) auth.logout();
      return throwError(() => error);
    }),
  );
};
