import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CreateUserRequest } from '../../core/models/role.model';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-customer-register',
  standalone: true,
  imports: [FormsModule, RouterLink],
  styles: [`
    :host input.username-available { border-color: #16803c; }
    :host input.username-unavailable { border-color: #b42318; }
  `],
  template: `
    <section class="admin-login-wrap">
      <form class="form-card admin-login" (ngSubmit)="submit()" #registerForm="ngForm">
        <a class="text-link" routerLink="/home">← Back to Pawan Dham</a>
        <p class="eyebrow">Begin your next journey</p><h1>Create your account</h1>
        @if (error()) { <div class="notice notice--error" role="alert">{{ error() }}</div> }
        @if (created()) { <div class="notice notice--success" role="status">Your customer account is ready. <a routerLink="/login">Sign in to continue</a>.</div> }
        <label>Full name<input name="name" [(ngModel)]="form.name" required autocomplete="name"></label>
        <label>Username<input name="username" [(ngModel)]="form.username" required autocomplete="username" readonly
          [class.username-available]="emailAvailable() === true && usernameAvailable() === true"
          [class.username-unavailable]="emailAvailable() === false || usernameAvailable() === false"></label>
        <label>Email address<input name="email" [(ngModel)]="form.email" (ngModelChange)="syncUsernameFromEmail($event)"
          (blur)="checkUsernameAvailability()" type="email" required autocomplete="email"></label>
        @if (usernameChecking()) { <p class="form-hint" role="status">Checking email, username, and mobile availability…</p> }
        @else if (availabilityError()) { <p class="notice notice--error" role="alert">{{ availabilityError() }}</p> }
        @else if (emailAvailable() === false) { <p class="notice notice--error" role="alert">This email is already registered for a customer account.</p> }
        @else if (mobileAvailable() === false) { <p class="notice notice--error" role="alert">This mobile number is already registered for a customer account.</p> }
        @else if (emailAvailable() === true && usernameAvailable() === true) {
          <p class="form-hint" role="status">{{ usernameWasGenerated() ? 'That username was taken. Suggested unique username: ' : 'Username available: ' }}<strong>{{ form.username }}</strong></p>
        }
        <label>Mobile number<input name="mobile" [(ngModel)]="form.mobile" (ngModelChange)="onMobileChange()"
          (blur)="checkUsernameAvailability()" type="tel" pattern="[0-9+() -]{10,16}" required autocomplete="tel"></label>
        <label>Password<input name="password" [(ngModel)]="form.password" type="password" minlength="8" required autocomplete="new-password"></label>
        <label>Gender
          <select name="gender" [(ngModel)]="form.gender" required>
            <option value="" disabled>Select gender</option>
            <option value="Male">Male</option>
            <option value="Female">Female</option>
            <option value="Others">Others</option>
          </select>
        </label>
        <button class="button" type="submit"
          [disabled]="registerForm.invalid || loading() || usernameChecking() || emailAvailable() !== true
            || usernameAvailable() !== true || mobileAvailable() !== true">
          {{ loading() ? 'Creating account…' : 'Create customer account' }}</button>
        <p class="form-hint">Already registered? <a routerLink="/login">Sign in</a>.</p>
      </form>
    </section>
  `,
})
export class CustomerRegister {
  private readonly auth = inject(AuthService);
  readonly loading = signal(false);
  readonly created = signal(false);
  readonly error = signal('');
  readonly usernameChecking = signal(false);
  readonly usernameAvailable = signal<boolean | null>(null);
  readonly emailAvailable = signal<boolean | null>(null);
  readonly mobileAvailable = signal<boolean | null>(null);
  readonly usernameWasGenerated = signal(false);
  readonly availabilityError = signal('');
  form: CreateUserRequest = { name: '', username: '', email: '', mobile: '', password: '', gender: '', role: 'CUSTOMER' };
  private availabilityRequestId = 0;

  syncUsernameFromEmail(email: string): void {
    this.form.username = email.split('@', 1)[0] ?? '';
    this.availabilityRequestId++;
    this.usernameChecking.set(false);
    this.usernameAvailable.set(null);
    this.emailAvailable.set(null);
    this.mobileAvailable.set(null);
    this.usernameWasGenerated.set(false);
    this.availabilityError.set('');
  }

  onMobileChange(): void {
    this.availabilityRequestId++;
    this.usernameChecking.set(false);
    this.usernameAvailable.set(null);
    this.emailAvailable.set(null);
    this.mobileAvailable.set(null);
    this.usernameWasGenerated.set(false);
    this.availabilityError.set('');
  }

  checkUsernameAvailability(): void {
    const email = this.form.email.trim();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return;

    const requestId = ++this.availabilityRequestId;
    this.usernameChecking.set(true);
    this.availabilityError.set('');
    const mobile = this.form.mobile.trim();
    this.auth.checkUsernameAvailability(email, 'CUSTOMER', this.form.username, mobile).subscribe({
      next: (availability) => {
        if (requestId !== this.availabilityRequestId || this.form.email.trim() !== email
            || this.form.mobile.trim() !== mobile) return;
        const baseUsername = email.split('@', 1)[0] ?? '';
        this.form.username = availability.username;
        this.usernameAvailable.set(availability.usernameAvailable);
        this.emailAvailable.set(availability.emailAvailable);
        this.mobileAvailable.set(availability.mobileAvailable);
        this.usernameWasGenerated.set(availability.username !== baseUsername);
        this.usernameChecking.set(false);
      },
      error: () => {
        if (requestId !== this.availabilityRequestId) return;
        this.usernameChecking.set(false);
        this.usernameAvailable.set(null);
        this.emailAvailable.set(null);
        this.mobileAvailable.set(null);
        this.availabilityError.set('Could not check availability. Edit the email and try again.');
      },
    });
  }

  submit(): void {
    if (this.emailAvailable() !== true || this.usernameAvailable() !== true
        || this.mobileAvailable() !== true || this.usernameChecking()) {
      this.error.set('Check email, username, and mobile availability before creating the account.');
      return;
    }
    this.error.set('');
    this.created.set(false);
    this.loading.set(true);
    const request = { ...this.form, role: 'CUSTOMER' as const };
    this.auth.register(request).subscribe({
      next: () => {
        this.loading.set(false);
        this.created.set(true);
        this.form = { name: '', username: '', email: '', mobile: '', password: '', gender: '', role: 'CUSTOMER' };
        this.availabilityRequestId++;
        this.usernameAvailable.set(null);
        this.emailAvailable.set(null);
        this.mobileAvailable.set(null);
        this.usernameWasGenerated.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        const message = this.getErrorMessage(error);
        this.error.set(error.status
          ? message
            ? `Could not create the account: ${message}`
            : `Could not create the account (${error.status}). Check the details and try again.`
          : 'Could not reach the account service.');
      },
    });
  }

  private getErrorMessage(error: HttpErrorResponse): string | null {
    const body: unknown = error.error;
    if (typeof body !== 'object' || body === null || !('message' in body)) {
      return null;
    }

    const message = body.message;
    return typeof message === 'string' && message.trim() ? message : null;
  }
}
