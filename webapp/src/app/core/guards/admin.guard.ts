import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { RoleService } from '../services/role.service';

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const roles = inject(RoleService);
  const router = inject(Router);
  if (roles.canManageContent(auth.role())) return true;
  return router.createUrlTree([auth.isAuthenticated() ? roles.dashboardPath(auth.role() ?? 'CUSTOMER') : '/admin/login']);
};
