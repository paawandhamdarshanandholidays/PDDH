import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { RoleService } from '../../core/services/role.service';
import { UserRole } from '../../core/models/role.model';

@Component({
  selector: 'app-admin-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <section class="admin-login-wrap">
      <form class="form-card admin-login" (ngSubmit)="submit()">
        <a class="text-link" routerLink="/home">← Back to the website</a>
        <p class="eyebrow">{{ fixedRole() === 'CEO' ? 'Executive access' : role === 'CUSTOMER' || fixedRole() === 'CUSTOMER' ? 'Traveller access' : 'Team access' }}</p><h1>{{ fixedRole() === 'CEO' ? 'CEO sign in' : role === 'CUSTOMER' || fixedRole() === 'CUSTOMER' ? 'Customer sign in' : 'Staff sign in' }}</h1><p class="admin-login__intro">{{ fixedRole() === 'CEO' ? 'Sign in to access every management area.' : role === 'CUSTOMER' || fixedRole() === 'CUSTOMER' ? 'Sign in to plan journeys and manage your profile.' : 'Choose your role and sign in to your workspace.' }}</p>
        @if (error()) { <div class="notice notice--error" role="alert">{{ error() }}</div> }
        <label>Username or email<input name="username" [(ngModel)]="username" required autocomplete="username"></label>
        <label>Password<input name="password" [(ngModel)]="password" required type="password" autocomplete="current-password"></label>
        @if (!fixedRole()) {
          <label>Account role
            <select name="role" [(ngModel)]="role" [disabled]="rolesLoading() || roleOptions().length === 0" required>
              @for (availableRole of roleOptions(); track availableRole) {
                <option [value]="availableRole">{{ roleLabel(availableRole) }}</option>
              }
            </select>
          </label>
          @if (rolesLoading()) { <p class="form-hint" role="status">Loading available roles…</p> }
          @if (rolesError()) { <p class="notice notice--error" role="alert">{{ rolesError() }}</p> }
        }
        <button class="button" type="submit" [disabled]="loading() || rolesLoading() || (!fixedRole() && roleOptions().length === 0)">{{ loading() ? 'Signing in…' : fixedRole() ? 'Sign in as ' + fixedRole()?.toLowerCase() : 'Sign in' }}</button>
        @if (fixedRole() === 'CUSTOMER') { <p class="form-hint">New to Pawan Dham? <a routerLink="/register">Create a customer account</a>.</p> }
        @if (fixedRole() === 'CEO') { <p class="form-hint">First-time setup? <a routerLink="/ceo/register">Register the initial CEO account</a>.</p> }
        @if (role === 'CUSTOMER' || fixedRole() === 'CUSTOMER') { <p class="form-hint"><a routerLink="/forgot-password">Forgot your password?</a></p> }
        <p class="form-hint">Authentication uses the configured Pawan Dham API at <code>/auth/login</code>.</p>
      </form>
    </section>
  `,
})
export class AdminLogin {
  username = '';
  password = '';
  role: UserRole = 'ADMIN';
  readonly fixedRole = signal<UserRole | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly rolesLoading = signal(false);
  readonly rolesError = signal('');
  readonly roleOptions = signal<UserRole[]>([]);

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly roles: RoleService,
    route: ActivatedRoute,
  ) {
    const routeRole = route.snapshot.data['role'];
    if (routeRole && ['CEO', 'CUSTOMER'].includes(routeRole)) {
      this.fixedRole.set(routeRole as UserRole);
      this.role = routeRole as UserRole;
    } else {
      this.rolesLoading.set(true);
      this.auth.availableRoles().subscribe({
        next: (roles) => {
          this.rolesLoading.set(false);
          if (!Array.isArray(roles) || !roles.every((candidate) =>
            ['CEO', 'ADMIN', 'MANAGER', 'STAFF', 'CUSTOMER'].includes(candidate))) {
            this.rolesError.set('The available roles response has an invalid format.');
            return;
          }
          this.roleOptions.set(roles);
          if (roles.length > 0) this.role = roles.includes('ADMIN') ? 'ADMIN' : roles[0];
        },
        error: (error: HttpErrorResponse) => {
          this.rolesLoading.set(false);
          this.rolesError.set(error.status
            ? `Could not load available roles (${error.status}).`
            : 'Could not reach the roles service.');
        },
      });
    }
  }

  roleLabel(role: UserRole): string {
    if (role === 'CEO') return 'CEO';
    if (role === 'ADMIN') return 'Administrator';
    if (role === 'CUSTOMER') return 'User';
    return role.charAt(0) + role.slice(1).toLowerCase();
  }

  submit(): void {
    if (!this.username.trim() || !this.password) {
      this.error.set('Enter your username and password.');
      return;
    }
    this.loading.set(true);
    this.error.set('');
    const role = this.fixedRole() ?? this.role;
    this.auth.login({ username: this.username.trim(), password: this.password, role }).subscribe({
      next: () => {
        this.loading.set(false);
        void this.router.navigate([this.roles.dashboardPath(role)]);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(error.status
          ? `Sign in failed (${error.status}). Check your credentials and API availability.`
          : 'Could not reach the sign-in service. Please try again.');
      },
    });
  }
}
