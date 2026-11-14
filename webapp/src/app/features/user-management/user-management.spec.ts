import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../core/services/auth.service';
import { UserManagementService } from '../../core/services/user-management.service';
import { UserManagement } from './user-management';

describe('UserManagement', () => {
  let fixture: ComponentFixture<UserManagement>;
  let listRoles: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    listRoles = vi.fn(() => of(['ADMIN', 'STAFF', 'CUSTOMER']));
    await TestBed.configureTestingModule({
      imports: [UserManagement],
      providers: [
        { provide: AuthService, useValue: { role: () => 'ADMIN', isCeo: () => false } },
        { provide: UserManagementService, useValue: { listRoles } },
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
      'Staff',
      'User',
    ]);
  });
});
