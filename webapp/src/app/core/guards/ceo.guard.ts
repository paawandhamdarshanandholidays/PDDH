import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const ceoGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isCeo()) return true;
  return router.createUrlTree([auth.isAuthenticated() ? '/home' : '/ceo/login']);
};
