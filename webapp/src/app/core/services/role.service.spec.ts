import { RoleService } from './role.service';

describe('RoleService', () => {
  const roles = new RoleService();

  it('allows administrators to create peer administrators and junior profiles, but not CEOs', () => {
    expect(roles.creatableRoles('ADMIN')).toEqual(['ADMIN', 'MANAGER', 'STAFF', 'CUSTOMER']);
    expect(roles.canCreateUser('ADMIN', 'ADMIN')).toBe(true);
    expect(roles.canCreateUser('ADMIN', 'CEO')).toBe(false);
  });

  it('allows CEOs to register peer CEOs and keeps other creation rights below the creator', () => {
    expect(roles.creatableRoles('CEO')).toEqual(['CEO', 'ADMIN', 'MANAGER', 'STAFF', 'CUSTOMER']);
    expect(roles.creatableRoles('MANAGER')).toEqual(['STAFF', 'CUSTOMER']);
    expect(roles.creatableRoles('STAFF')).toEqual(['CUSTOMER']);
    expect(roles.creatableRoles('CUSTOMER')).toEqual([]);
  });
});
