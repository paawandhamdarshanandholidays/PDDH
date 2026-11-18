import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../core/services/auth.service';
import { UserManagementService } from '../../core/services/user-management.service';
import { UserRole } from '../../core/models/role.model';
import { UserManagement } from './user-management';

describe('UserManagement', () => {
  let fixture: ComponentFixture<UserManagement>;
  let listRoles: ReturnType<typeof vi.fn>;
  let listUsers: ReturnType<typeof vi.fn>;
  let updateUser: ReturnType<typeof vi.fn>;
  let setUserActive: ReturnType<typeof vi.fn>;
  let deleteUser: ReturnType<typeof vi.fn>;
  let actorRole: string;

  beforeEach(async () => {
    actorRole = 'ADMIN';
    listRoles = vi.fn(() => of(['CEO', 'ADMIN', 'MANAGER', 'STAFF', 'CUSTOMER']));
    listUsers = vi.fn((role: UserRole) => of({
      content: role === 'STAFF' ? [{
        id: 'staff-1',
        name: 'Staff User',
        username: 'staff',
        email: 'staff@example.test',
        mobile: '123',
        role: 'STAFF' as const,
        active: true,
        lastLoginAt: null,
      }] : [],
      page: 0,
      size: 50,
      totalElements: role === 'STAFF' ? 1 : 0,
      totalPages: role === 'STAFF' ? 1 : 0,
    }));
    updateUser = vi.fn((id: string, update: { name: string; email: string; mobile: string; role?: UserRole }) => of({
      id,
      name: update.name,
      username: 'staff',
      email: update.email,
      mobile: update.mobile,
      role: update.role ?? 'STAFF',
      active: true,
      lastLoginAt: null,
    }));
    setUserActive = vi.fn((id: string, active: boolean) => of({
      id,
      name: 'Staff User',
      username: 'staff',
      email: 'staff@example.test',
      mobile: '123',
      role: 'STAFF' as const,
      active,
      lastLoginAt: null,
    }));
    deleteUser = vi.fn(() => of(void 0));
    await TestBed.configureTestingModule({
      imports: [UserManagement],
      providers: [
        { provide: AuthService, useValue: { role: () => actorRole, isCeo: () => actorRole === 'CEO' } },
        { provide: UserManagementService, useValue: { listRoles, listUsers, updateUser, setUserActive, deleteUser } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserManagement);
    fixture.detectChanges();
  });

  it('fills the profile role dropdown from the roles API response', () => {
    expect(listRoles).toHaveBeenCalledOnce();
    const options = Array.from(
      fixture.nativeElement.querySelectorAll('select[name="profileRole"] option'),
    ) as HTMLOptionElement[];

    expect(options.map((option) => option.textContent?.trim())).toEqual([
      'Choose a role',
      'Admin',
      'Manager',
      'Staff',
      'User',
    ]);
  });

  it('allows administrators to view and register manager and staff profiles', () => {
    for (const role of ['MANAGER', 'STAFF'] as const) {
      fixture.componentInstance.selectRole(role);
      expect(fixture.componentInstance.canViewSelectedRole()).toBe(true);
      expect(fixture.componentInstance.canRegisterSelectedRole()).toBe(true);
    }
  });

  it('lets managers view their permitted staff profiles using the 50-row profile page API', () => {
    actorRole = 'MANAGER';
    fixture.detectChanges();
    fixture.componentInstance.selectRole('STAFF');
    fixture.componentInstance.selectAction('view');
    fixture.detectChanges();

    expect(listUsers).toHaveBeenCalledWith('STAFF', 0, 50);
    expect(fixture.componentInstance.loading()).toBe(false);
    expect(fixture.componentInstance.selectedAction()).toBe('view');
    expect(fixture.nativeElement.textContent).toContain('Staff User');
  });

  it('updates, deactivates, and deletes selected central profiles', () => {
    actorRole = 'MANAGER';
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.selectRole('STAFF');
    component.selectAction('view');
    const user = component.managedUsers()[0]!;
    expect(component.canManageProfile(user)).toBe(true);

    component.edit(user);
    component.editDraft.name = 'Updated Staff';
    component.saveEdit(user);
    expect(updateUser).toHaveBeenCalledWith('staff-1', expect.objectContaining({ name: 'Updated Staff' }));

    component.toggleActive(component.managedUsers()[0]!);
    expect(setUserActive).toHaveBeenCalledWith('staff-1', false);

    vi.stubGlobal('confirm', () => true);
    component.deleteUser(component.managedUsers()[0]!);
    expect(deleteUser).toHaveBeenCalledWith('staff-1');
    vi.unstubAllGlobals();
  });
});
