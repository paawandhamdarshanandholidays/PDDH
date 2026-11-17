import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { RoleService } from '../services/role.service';

export const roleDashboardGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const roles = inject(RoleService);
  const expected = route.data['role'];
  const actual = auth.role();
  if (auth.isAuthenticated() && actual === expected) return true;
  return router.createUrlTree([auth.isAuthenticated() && actual ? roles.dashboardPath(actual) : '/login']);
};

export const userManagementGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const roles = inject(RoleService);
  if (auth.isAuthenticated() && roles.manageableRoles(auth.role()).length) return true;
  return router.createUrlTree([auth.isAuthenticated() ? roles.dashboardPath(auth.role() ?? 'CUSTOMER') : '/admin/login']);
};

export const bookingsGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const roles = inject(RoleService);
  if (auth.isAuthenticated() && roles.canManageBookings(auth.role())) return true;
  return router.createUrlTree([auth.isAuthenticated() ? roles.dashboardPath(auth.role() ?? 'CUSTOMER') : '/login']);
};

export const authenticatedGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const roles = inject(RoleService);
  if (auth.isAuthenticated() && auth.role()) return true;
  return router.createUrlTree([auth.isAuthenticated() ? roles.dashboardPath('CUSTOMER') : '/login']);
};

export const customerBookingGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const roles = inject(RoleService);
  const actual = auth.role();
  if (auth.isAuthenticated() && actual === 'CUSTOMER') return true;
  return router.createUrlTree([auth.isAuthenticated() && actual
    ? roles.dashboardPath(actual)
    : '/login']);
};
