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
    await TestBed.configureTestingModule({
      imports: [UserManagement],
      providers: [
        { provide: AuthService, useValue: { role: () => actorRole, isCeo: () => actorRole === 'CEO' } },
        { provide: UserManagementService, useValue: { listRoles, listUsers } },
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
});
