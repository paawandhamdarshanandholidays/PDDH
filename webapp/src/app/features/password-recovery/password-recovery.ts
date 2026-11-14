import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-password-recovery',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <section class="admin-login-wrap">
      <form class="form-card admin-login" (ngSubmit)="submit()">
        <a class="text-link" routerLink="/login">← Back to customer sign in</a>
        <p class="eyebrow">Account recovery</p><h1>Reset customer password</h1>
        @if (error()) { <div class="notice notice--error" role="alert">{{ error() }}</div> }
        @if (message()) { <div class="notice notice--success" role="status">{{ message() }}</div> }
        @if (!codeSent()) {
          <p>We’ll send a short-lived reset code to the email address on your customer account.</p>
          <label>Email address<input name="email" [(ngModel)]="email" type="email" required autocomplete="email"></label>
          <button class="button" type="submit" [disabled]="loading()">{{ loading() ? 'Sending…' : 'Send reset code' }}</button>
        } @else {
          <label>Email address<input name="confirmEmail" [(ngModel)]="email" type="email" required autocomplete="email"></label>
          <label>Reset code<input name="code" [(ngModel)]="code" required inputmode="numeric" autocomplete="one-time-code"></label>
          <label>New password<input name="newPassword" [(ngModel)]="newPassword" type="password" minlength="8" required autocomplete="new-password"></label>
          <button class="button" type="submit" [disabled]="loading()">{{ loading() ? 'Resetting…' : 'Reset password' }}</button>
        }
      </form>
    </section>
  `,
})
export class PasswordRecovery {
  private readonly auth = inject(AuthService);
  readonly loading = signal(false);
  readonly codeSent = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  email = '';
  code = '';
  newPassword = '';

  submit(): void {
    this.error.set('');
    this.message.set('');
    this.loading.set(true);
    const request = this.codeSent()
      ? this.auth.confirmPasswordResetOtp({
          email: this.email.trim(),
          code: this.code.trim(),
          newPassword: this.newPassword,
        })
      : this.auth.requestPasswordResetOtp({ email: this.email.trim() });
    request.subscribe({
      next: (response) => {
        this.loading.set(false);
        if (!this.codeSent()) {
          this.codeSent.set(true);
          this.message.set('If an active customer account matches that email, a reset code has been sent.');
        } else {
          this.message.set(response.message);
          this.codeSent.set(false);
          this.code = '';
          this.newPassword = '';
        }
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(error.status
          ? `Could not complete password recovery (${error.status}).`
          : 'Could not reach the password recovery service.');
      },
    });
  }
}
