import { Injectable } from '@angular/core';
import { UserRole } from '../models/role.model';

const roleRank: Record<UserRole, number> = {
  CEO: 0,
  ADMIN: 1,
  MANAGER: 2,
  STAFF: 3,
  CUSTOMER: 4,
};

@Injectable({ providedIn: 'root' })
export class RoleService {
  canManage(actor: UserRole | null, target: UserRole): boolean {
    return actor !== null && roleRank[actor] < roleRank[target];
  }

  canCreateUser(actor: UserRole | null, target: UserRole): boolean {
    if ((actor === 'ADMIN' || actor === 'CEO') && actor === target) return true;
    return this.canManage(actor, target);
  }

  creatableRoles(actor: UserRole | null): UserRole[] {
    if (!actor) return [];
    return Object.keys(roleRank)
      .map((role) => role as UserRole)
      .filter((role) => this.canCreateUser(actor, role));
  }

  manageableRoles(actor: UserRole | null): UserRole[] {
    if (!actor) return [];
    return Object.keys(roleRank)
      .map((role) => role as UserRole)
      .filter((role) => this.canManage(actor, role));
  }

  canManageContent(role: UserRole | null): boolean {
    return role === 'CEO' || role === 'ADMIN' || role === 'MANAGER';
  }

  canManageBookings(role: UserRole | null): boolean {
    return role === 'CEO' || role === 'ADMIN' || role === 'MANAGER' || role === 'STAFF';
  }

  dashboardPath(role: UserRole): string {
    const paths: Record<UserRole, string> = {
      CEO: '/ceo/dashboard',
      ADMIN: '/admin/dashboard',
      MANAGER: '/manager/dashboard',
      STAFF: '/staff/dashboard',
      CUSTOMER: '/customer/dashboard',
    };
    return paths[role];
  }
}
