import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { UserProfile } from '../../core/models/role.model';
import { UserManagementService } from '../../core/services/user-management.service';

@Component({
  selector: 'app-customer-profile',
  standalone: true,
  imports: [FormsModule],
  template: `
    <section class="admin-page profile-page">
      <div class="admin-heading"><div><p class="eyebrow">Your account</p><h1>My profile</h1><p>Keep your contact details up to date for booking confirmations.</p></div></div>
      @if (error()) { <div class="notice notice--error" role="alert">{{ error() }}</div> }
      @if (message()) { <div class="notice notice--success" role="status">{{ message() }}</div> }
      @if (profile(); as user) {
        <form class="form-card profile-form" (ngSubmit)="save()" #profileForm="ngForm">
          <label>Full name<input name="name" [(ngModel)]="draft.name" required autocomplete="name"></label>
          <label>Username<input [value]="user.username" readonly></label>
          <label>Email address<input name="email" [(ngModel)]="draft.email" type="email" required autocomplete="email"></label>
          <label>Mobile number<input name="mobile" [(ngModel)]="draft.mobile" type="tel" required autocomplete="tel"></label>
          <label>Account role<input [value]="user.role" readonly></label>
          <button class="button" type="submit" [disabled]="profileForm.invalid || saving()">{{ saving() ? 'Saving…' : 'Save changes' }}</button>
        </form>
        <form class="form-card profile-form" (ngSubmit)="resetPassword()" #passwordForm="ngForm">
          <p class="eyebrow">Account security</p><h2>Reset password</h2>
          @if (user.role === 'CUSTOMER') {
            <label>Current password<input name="currentPassword" [(ngModel)]="passwordDraft.currentPassword" type="password" required autocomplete="current-password"></label>
          } @else {
            <p class="form-hint">Password changes for team accounts take effect only after CEO approval.</p>
          }
          <label>New password<input name="newPassword" [(ngModel)]="passwordDraft.newPassword" type="password" minlength="8" required autocomplete="new-password"></label>
          <button class="button" type="submit" [disabled]="passwordForm.invalid || saving()">{{ saving() ? 'Submitting…' : user.role === 'CUSTOMER' ? 'Change password' : 'Request CEO approval' }}</button>
        </form>
      } @else if (loading()) { <p>Loading your profile…</p> }
    </section>
  `,
})
export class CustomerProfile {
  private readonly api = inject(UserManagementService);
  readonly profile = signal<UserProfile | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  draft = { name: '', email: '', mobile: '' };
  passwordDraft = { currentPassword: '', newPassword: '' };

  constructor() {
    this.load();
  }

  load(): void {
    this.api.getMyProfile().subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.draft = { name: profile.name, email: profile.email, mobile: profile.mobile };
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(error.status
          ? `Could not load your profile (${error.status}).`
          : 'Could not reach the profile service.');
      },
    });
  }

  save(): void {
    this.error.set('');
    this.message.set('');
    this.saving.set(true);
    this.api.updateMyProfile(this.draft).subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.saving.set(false);
        this.message.set('Your profile has been updated.');
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        this.error.set(error.status
          ? `Could not update your profile (${error.status}).`
          : 'Could not reach the profile service.');
      },
    });
  }

  resetPassword(): void {
    const user = this.profile();
    if (!user) return;
    this.error.set('');
    this.message.set('');
    this.saving.set(true);
    if (user.role === 'CUSTOMER') {
      this.api.changeMyPassword(this.passwordDraft.currentPassword, this.passwordDraft.newPassword).subscribe({
        next: (response) => this.passwordResetComplete(response.message),
        error: (error: HttpErrorResponse) => this.passwordResetFailed(error),
      });
    } else {
      this.api.requestPasswordReset({ newPassword: this.passwordDraft.newPassword }).subscribe({
        next: (response) => this.passwordResetComplete(
          `Your password reset request was sent to the CEO for approval (request ${response.id}).`,
        ),
        error: (error: HttpErrorResponse) => this.passwordResetFailed(error),
      });
    }
  }

  private passwordResetComplete(message: string): void {
        this.saving.set(false);
        this.passwordDraft = { currentPassword: '', newPassword: '' };
        this.message.set(message);
  }

  private passwordResetFailed(error: HttpErrorResponse): void {
    this.saving.set(false);
    this.error.set(error.status
      ? `Could not reset the password (${error.status}).`
      : 'Could not reach the password service.');
  }
}
