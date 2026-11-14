import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../core/services/auth.service';
import { CustomerRegister } from './customer-register';

describe('CustomerRegister', () => {
  let fixture: ComponentFixture<CustomerRegister>;
  let component: CustomerRegister;
  let auth: {
    checkUsernameAvailability: ReturnType<typeof vi.fn>;
    register: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    auth = {
      checkUsernameAvailability: vi.fn(),
      register: vi.fn(),
    };
    await TestBed.configureTestingModule({
      imports: [CustomerRegister],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerRegister);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('generates a unique email-based username and marks it available', () => {
    auth.checkUsernameAvailability.mockReturnValue(of({
      username: 'jane2',
      usernameAvailable: true,
      emailAvailable: true,
      mobileAvailable: true,
    }));
    component.form.email = 'jane@example.com';
    component.syncUsernameFromEmail(component.form.email);

    component.checkUsernameAvailability();
    fixture.detectChanges();

    expect(auth.checkUsernameAvailability).toHaveBeenCalledWith(
      'jane@example.com', 'CUSTOMER', 'jane', '',
    );
    const usernameInput = fixture.nativeElement.querySelector('input[name="username"]') as HTMLInputElement;
    expect(component.form.username).toBe('jane2');
    expect(usernameInput.classList.contains('username-available')).toBe(true);
    expect(component.usernameWasGenerated()).toBe(true);
  });

  it('marks an email already registered for the customer role unavailable', () => {
    auth.checkUsernameAvailability.mockReturnValue(of({
      username: 'jane',
      usernameAvailable: true,
      emailAvailable: false,
      mobileAvailable: true,
    }));
    component.form.email = 'jane@example.com';
    component.syncUsernameFromEmail(component.form.email);

    component.checkUsernameAvailability();
    fixture.detectChanges();

    const usernameInput = fixture.nativeElement.querySelector('input[name="username"]') as HTMLInputElement;
    expect(usernameInput.classList.contains('username-unavailable')).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('already registered for a customer account');
  });

  it('checks mobile availability in the same request as email and username', () => {
    auth.checkUsernameAvailability.mockReturnValue(of({
      username: 'jane',
      usernameAvailable: true,
      emailAvailable: true,
      mobileAvailable: false,
    }));
    component.form.email = 'jane@example.com';
    component.form.mobile = '1234567890';
    component.syncUsernameFromEmail(component.form.email);

    component.checkUsernameAvailability();
    fixture.detectChanges();

    expect(auth.checkUsernameAvailability).toHaveBeenCalledWith(
      'jane@example.com', 'CUSTOMER', 'jane', '1234567890',
    );
    expect(component.mobileAvailable()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('mobile number is already registered');
  });
});
