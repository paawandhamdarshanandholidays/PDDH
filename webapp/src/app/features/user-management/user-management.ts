import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { RoleService } from '../../core/services/role.service';
import { CreateUserRequest, USER_ROLES, UserProfile, UserRole } from '../../core/models/role.model';
import { ProfileUpdate, UserManagementService } from '../../core/services/user-management.service';
import { PasswordResetApproval } from '../../core/services/user-management.service';
import { IdempotencySubmission } from '../../core/services/idempotency-key';

const emptyUser = (role: UserRole): CreateUserRequest => ({
  name: '',
  username: '',
  email: '',
  mobile: '',
  password: '',
  gender: '',
  role,
});

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [FormsModule, DatePipe],
  styles: [`
    :host input.username-available { border-color: #16803c; }
    :host input.username-unavailable { border-color: #b42318; }
  `],
  template: `
    <section class="admin-page">
      <div class="admin-heading"><div><p class="eyebrow">People & access</p><h1>Manage team profiles</h1><p>Select a role, then choose whether to view existing profiles or register a new one. Profiles load only when you choose View.</p></div></div>
      <div class="role-ladder" aria-label="Role hierarchy"><span>CEO</span><b>›</b><span>Administrator</span><b>›</b><span>Manager</span><b>›</b><span>Staff</span><b>›</b><span>User</span></div>
      @if (error()) { <div class="notice notice--error" role="alert">{{ error() }}</div> }
      @if (message()) { <div class="notice notice--success" role="status">{{ message() }}</div> }
      @if (loading()) { <div class="notice">Loading profiles…</div> }
      @if (isCeo()) {
        <section class="user-list-panel profile-role-panel">
          <div class="list-heading"><div><p class="eyebrow">Security approvals</p><h2>Pending password resets</h2></div><button class="text-button" type="button" (click)="loadPendingResets()">Refresh</button></div>
          @for (request of pendingResets(); track request.id) {
            <article class="user-card">
              <div class="user-card__head"><div><span class="eyebrow">Requested by {{ request.requesterName }}</span><h3>{{ request.targetName }}</h3></div><span class="user-state">Pending</span></div>
              <p>{{ request.targetUsername }} · {{ request.requestedAt | date:'medium' }}</p>
              <div class="user-card__actions">
                <button class="button button--small" type="button" (click)="reviewReset(request, true)">Approve reset</button>
                <button class="text-button" type="button" (click)="reviewReset(request, false)">Reject</button>
              </div>
            </article>
          } @empty { <p class="form-hint">There are no pending password resets.</p> }
        </section>
      }
      <section class="user-list-panel">
        <div class="list-heading"><div><p class="eyebrow">People & access</p><h2>Choose a profile role</h2></div></div>
        <div class="profile-browser">
          <label>Profile role
            <select name="profileRole" [ngModel]="selectedProfileRole()" (ngModelChange)="selectRole($event)">
              <option [ngValue]="null">Choose a role</option>
              @if (rolesLoading()) { <option disabled>Loading roles…</option> }
              @for (role of profileRoles(); track role) { <option [ngValue]="role">{{ roleLabel(role) }}</option> }
            </select>
          </label>
          <button class="button button--small" type="button"
            [disabled]="!selectedProfileRole() || !canViewSelectedRole() || loading()"
            (click)="selectAction('view')">View profiles</button>
          @if (selectedProfileRole() !== 'CEO' || ceoProfileExists() === false) {
            <button class="button button--small" type="button"
              [disabled]="!selectedProfileRole() || !canRegisterSelectedRole() || loading()"
              (click)="selectAction('register')">Register profile</button>
          }
        </div>
        @if (selectedProfileRole() && !canViewSelectedRole() && !canRegisterSelectedRole()) {
          <p class="form-hint">Your role cannot view or register {{ roleLabel(selectedProfileRole()!).toLowerCase() }} profiles.</p>
        }
      </section>

      @if (selectedProfileRole() && selectedAction() === 'register') {
        <form class="form-card" (ngSubmit)="create()" #newUserForm="ngForm">
          <p class="eyebrow">Register {{ roleLabel(selectedProfileRole()!) }}</p><h2>Create a {{ roleLabel(selectedProfileRole()!).toLowerCase() }} profile</h2>
          <label>Full name<input name="name" [(ngModel)]="newUser.name" required autocomplete="name"></label>
          <label>Username<input name="username" [(ngModel)]="newUser.username" required autocomplete="off" readonly
            [class.username-available]="emailAvailable() === true && usernameAvailable() === true"
            [class.username-unavailable]="emailAvailable() === false || usernameAvailable() === false"></label>
          <label>Email<input name="email" [(ngModel)]="newUser.email" (ngModelChange)="syncUsernameFromEmail($event)"
            (blur)="checkUsernameAvailability()" required type="email" autocomplete="email"></label>
          @if (usernameChecking()) { <p class="form-hint" role="status">Checking email, username, and mobile availability…</p> }
          @else if (availabilityError()) { <p class="notice notice--error" role="alert">{{ availabilityError() }}</p> }
          @else if (emailAvailable() === false) { <p class="notice notice--error" role="alert">This email is already registered for this role.</p> }
          @else if (mobileAvailable() === false) { <p class="notice notice--error" role="alert">This mobile number is already registered for this role.</p> }
          @else if (emailAvailable() === true && usernameAvailable() === true) {
            <p class="form-hint" role="status">{{ usernameWasGenerated() ? 'That username was taken. Suggested unique username: ' : 'Username available: ' }}<strong>{{ newUser.username }}</strong></p>
          }
          <label>Mobile number<input name="mobile" [(ngModel)]="newUser.mobile" (ngModelChange)="onMobileChange()"
            (blur)="checkUsernameAvailability()" required type="tel" autocomplete="tel"></label>
          <label>Temporary password<input name="password" [(ngModel)]="newUser.password" required type="password" minlength="8" autocomplete="new-password"></label>
          <label>Gender
            <select name="gender" [(ngModel)]="newUser.gender" required>
              <option value="" disabled>Select gender</option>
              <option value="Male">Male</option>
              <option value="Female">Female</option>
              <option value="Others">Others</option>
            </select>
          </label>
          <button class="button" type="submit"
            [disabled]="newUserForm.invalid || loading() || submittingUser() || usernameChecking()
              || emailAvailable() !== true || usernameAvailable() !== true || mobileAvailable() !== true">Create profile</button>
        </form>
      }

      @if (selectedProfileRole() && selectedAction() === 'view') {
        <div class="user-list-panel">
          <div class="list-heading"><div><p class="eyebrow">Browse profiles</p><h2>{{ roleLabel(selectedProfileRole()!) }} profiles</h2></div></div>
          <div class="profile-browser">
            <label>Profiles to load
              <select name="profileCount" [(ngModel)]="requestedProfileCount">
                @for (count of profileCountOptions; track count) { <option [ngValue]="count">{{ count }}</option> }
              </select>
            </label>
            <button class="button button--small" type="button" [disabled]="loading()" (click)="load()">Load selected profiles</button>
          </div>
          @if (selectedProfileRole()) {
            <p class="form-hint">
              Loaded {{ profilesLoaded() }} of {{ profileLoadTarget() }} profiles ({{ totalProfiles() }} total).
              Each API request fetches at most 50 profiles.
            </p>
          }
          <div class="user-card-grid">
          @for (user of managedUsers(); track user.id) {
            <article class="user-card">
              @if (editingId() === user.id) {
                <form class="user-edit-form" (ngSubmit)="saveEdit(user)">
                  <label>Name<input name="editName" [(ngModel)]="editDraft.name" required></label>
                  <label>Email<input name="editEmail" [(ngModel)]="editDraft.email" type="email" required></label>
                  <label>Mobile<input name="editMobile" [(ngModel)]="editDraft.mobile" type="tel" required></label>
                  <label>Role<select name="editRole" [(ngModel)]="editDraft.role" required>@for (role of editableRoles(user); track role) { <option [value]="role">{{ roleLabel(role) }}</option> }</select></label>
                  <div class="user-card__actions"><button class="button button--small" type="submit">Save profile</button><button class="text-button" type="button" (click)="editingId.set(null)">Cancel</button></div>
                </form>
              } @else {
                <div class="user-card__head">
                  <div class="user-card__identity">
                    <span class="user-avatar" aria-hidden="true">{{ initials(user.name) }}</span>
                    <div><span class="role-badge" [class]="'role-badge role-badge--' + user.role.toLowerCase()">{{ roleLabel(user.role) }}</span><h3>{{ user.name }}</h3></div>
                  </div>
                  <span class="profile-status" [class.profile-status--inactive]="!user.active"><span class="profile-status__dot"></span>{{ user.active ? 'Active' : 'Inactive' }}</span>
                </div>
                <p>{{ user.email }} · {{ user.mobile }}</p><small>&#64;{{ user.username }}</small>
                <p class="form-hint">Last login: {{ user.lastLoginAt ? (user.lastLoginAt | date:'medium') : 'Never' }}</p>
                <div class="user-card__actions">
                  @if (canManageProfile(user)) {
                    <button class="text-button" type="button" (click)="edit(user)">Edit profile</button>
                    <button class="text-button" type="button" (click)="toggleActive(user)">{{ user.active ? 'Deactivate' : 'Reactivate' }}</button>
                    <button class="text-button" type="button" (click)="deleteUser(user)">Delete profile</button>
                  }
                </div>
              }
            </article>
          } @empty {
            @if (!loading()) { <div class="empty-state"><h3>No {{ roleLabel(selectedProfileRole()!) }} profiles found</h3><p>Try another role or register a profile.</p></div> }
          }
          </div>
        </div>
      }
    </section>
  `,
})
export class UserManagement {
  private readonly api = inject(UserManagementService);
  private readonly submission = new IdempotencySubmission();
  private readonly auth = inject(AuthService);
  private readonly roleService = inject(RoleService);
  readonly manageableRoles = computed(() => this.roleService.manageableRoles(this.auth.role()));
  readonly creatableRoles = computed(() => this.roleService.creatableRoles(this.auth.role()));
  readonly profileRoles = signal<UserRole[]>([]);
  readonly rolesLoading = signal(false);
  readonly profileCountOptions = [50, 100, 500, 1000, 5000, 10000];
  readonly selectedProfileRole = signal<UserRole | null>(null);
  readonly selectedAction = signal<'view' | 'register' | null>(null);
  readonly managedUsers = signal<UserProfile[]>([]);
  readonly profilesLoaded = signal(0);
  readonly totalProfiles = signal(0);
  readonly pendingResets = signal<PasswordResetApproval[]>([]);
  readonly loading = signal(false);
  readonly submittingUser = signal(false);
  readonly usernameChecking = signal(false);
  readonly usernameAvailable = signal<boolean | null>(null);
  readonly emailAvailable = signal<boolean | null>(null);
  readonly mobileAvailable = signal<boolean | null>(null);
  readonly usernameWasGenerated = signal(false);
  readonly availabilityError = signal('');
  readonly error = signal('');
  readonly message = signal('');
  readonly editingId = signal<string | null>(null);
  readonly isCeo = computed(() => this.auth.role() === 'CEO');
  readonly ceoProfileExists = signal<boolean | null>(null);
  requestedProfileCount = 50;
  newUser = emptyUser(this.creatableRoles()[0] ?? 'CUSTOMER');
  editDraft: ProfileUpdate = { name: '', email: '', mobile: '' };
  private profileRequestId = 0;
  private availabilityRequestId = 0;

  constructor() {
    this.loadRoles();
    if (this.isCeo()) {
      this.loadPendingResets();
      this.api.listUsers('CEO', 0, 1).subscribe({
        next: (response) => this.ceoProfileExists.set(response.totalElements > 0),
        error: () => this.ceoProfileExists.set(true),
      });
    }
  }

  private loadRoles(): void {
    this.rolesLoading.set(true);
    this.api.listRoles().subscribe({
      next: (roles) => {
        const actor = this.auth.role();
        const permittedRoles = new Set<UserRole>([
          ...this.manageableRoles(),
          ...this.creatableRoles(),
          ...(actor ? [actor] : []),
        ]);
        this.profileRoles.set([...new Set(
          roles.filter((role): role is UserRole => USER_ROLES.includes(role) && permittedRoles.has(role)),
        )]);
        this.rolesLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.rolesLoading.set(false);
        this.error.set(error.status
          ? `Could not load available roles (${error.status}).`
          : 'Could not reach the roles API.');
      },
    });
  }

  roleLabel(role: UserRole): string {
    if (role === 'CUSTOMER') return 'User';
    return role.charAt(0) + role.slice(1).toLowerCase();
  }

  initials(name: string | null | undefined): string {
    const parts = (name ?? '').trim().split(/\s+/).filter(Boolean);
    return parts.slice(0, 2).map((part) => part.charAt(0).toUpperCase()).join('') || '?';
  }

  syncUsernameFromEmail(email: string): void {
    this.newUser.username = email.split('@', 1)[0] ?? '';
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
    const email = this.newUser.email.trim();
    const role = this.selectedProfileRole();
    if (!role || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return;

    const mobile = this.newUser.mobile.trim();
    const requestId = ++this.availabilityRequestId;
    this.usernameChecking.set(true);
    this.availabilityError.set('');
    this.auth.checkUsernameAvailability(email, role, this.newUser.username, mobile).subscribe({
      next: (availability) => {
        if (requestId !== this.availabilityRequestId || this.newUser.email.trim() !== email
            || this.newUser.mobile.trim() !== mobile || this.selectedProfileRole() !== role) return;
        const baseUsername = email.split('@', 1)[0] ?? '';
        this.newUser.username = availability.username;
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

  editableRoles(user: UserProfile): UserRole[] {
    const availableRoles = this.profileRoles();
    const manageableRoles = this.manageableRoles().filter((role) => availableRoles.includes(role));
    return manageableRoles.includes(user.role) ? manageableRoles : [user.role, ...manageableRoles];
  }

  canViewSelectedRole(): boolean {
    const role = this.selectedProfileRole();
    return role !== null && (this.manageableRoles().includes(role)
      || this.creatableRoles().includes(role)
      || this.auth.role() === role);
  }

  canRegisterSelectedRole(): boolean {
    const role = this.selectedProfileRole();
    return role !== null && this.creatableRoles().includes(role);
  }

  canManageProfile(user: UserProfile): boolean {
    return this.roleService.canManage(this.auth.role(), user.role);
  }

  profileLoadTarget(): number {
    return Math.min(this.requestedProfileCount, this.totalProfiles());
  }

  selectRole(role: UserRole | null): void {
    this.availabilityRequestId++;
    this.usernameChecking.set(false);
    this.usernameAvailable.set(null);
    this.emailAvailable.set(null);
    this.mobileAvailable.set(null);
    this.usernameWasGenerated.set(false);
    this.availabilityError.set('');
    this.selectedProfileRole.set(role);
    this.selectedAction.set(null);
    this.managedUsers.set([]);
    this.profilesLoaded.set(0);
    this.totalProfiles.set(0);
    this.profileRequestId++;
    this.loading.set(false);
    if (role && this.canRegisterSelectedRole()) this.newUser = emptyUser(role);
  }

  selectAction(action: 'view' | 'register'): void {
    if (action === 'view') {
      if (!this.canViewSelectedRole()) return;
      this.selectedAction.set('view');
      this.load();
      return;
    }
    if (!this.canRegisterSelectedRole()) return;
    this.profileRequestId++;
    this.availabilityRequestId++;
    this.usernameChecking.set(false);
    this.usernameAvailable.set(null);
    this.emailAvailable.set(null);
    this.mobileAvailable.set(null);
    this.usernameWasGenerated.set(false);
    this.availabilityError.set('');
    this.loading.set(false);
    this.managedUsers.set([]);
    this.selectedAction.set('register');
    this.newUser = emptyUser(this.selectedProfileRole()!);
  }

  load(): void {
    const role = this.selectedProfileRole();
    if (!role) return;
    const requestId = ++this.profileRequestId;
    this.managedUsers.set([]);
    this.profilesLoaded.set(0);
    this.totalProfiles.set(0);
    this.loading.set(true);
    this.error.set('');
    this.loadProfileChunk(role, this.requestedProfileCount, 0, 0, requestId);
  }

  private loadProfileChunk(
    role: UserRole,
    requestedCount: number,
    page: number,
    loaded: number,
    requestId: number,
  ): void {
    const size = Math.min(50, requestedCount - loaded);
    this.api.listUsers(role, page, size).subscribe({
      next: (response) => {
        if (requestId !== this.profileRequestId) return;
        this.managedUsers.update((users) => [...users, ...response.content]);
        const nextLoaded = loaded + response.content.length;
        this.profilesLoaded.set(nextLoaded);
        this.totalProfiles.set(response.totalElements);
        if (response.content.length > 0 && nextLoaded < Math.min(requestedCount, response.totalElements)) {
          this.loadProfileChunk(role, requestedCount, page + 1, nextLoaded, requestId);
          return;
        }
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        if (requestId !== this.profileRequestId) return;
        this.loading.set(false);
        this.error.set(error.status
          ? `Could not load ${this.roleLabel(role).toLowerCase()} profiles (${error.status}).`
          : 'Could not reach the profiles API.');
      },
    });
  }

  create(): void {
    this.error.set('');
    this.message.set('');
    const selectedRole = this.selectedProfileRole();
    if (!selectedRole || this.selectedAction() !== 'register'
        || !this.roleService.canCreateUser(this.auth.role(), selectedRole)) {
      this.error.set('You do not have permission to create an account with that role.');
      return;
    }
    const createdRole = selectedRole;
    if (this.emailAvailable() !== true || this.usernameAvailable() !== true
        || this.mobileAvailable() !== true || this.usernameChecking()) {
      this.error.set('Check email, username, and mobile availability before creating the profile.');
      return;
    }
    const request = { ...this.newUser, role: createdRole };
    this.submittingUser.set(true);
    this.api.createUser(request, this.submission.forPayload(request)).subscribe({
      next: () => {
        this.submittingUser.set(false);
        this.message.set(`${this.roleLabel(createdRole)} profile created.`);
        this.newUser = emptyUser(this.creatableRoles()[0] ?? 'CUSTOMER');
        this.submission.clear();
        this.selectedAction.set('view');
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.submittingUser.set(false);
        const message = this.getErrorMessage(error);
        this.error.set(error.status
          ? message
            ? `Could not create the profile: ${message}`
            : `Could not create the profile (${error.status}).`
          : 'Could not reach the account service.');
      },
    });
  }

  private getErrorMessage(error: HttpErrorResponse): string | null {
    const body: unknown = error.error;
    if (typeof body !== 'object' || body === null || !('message' in body)) return null;
    const message = body.message;
    return typeof message === 'string' && message.trim() ? message : null;
  }

  edit(user: UserProfile): void {
    this.editingId.set(user.id);
    this.editDraft = { name: user.name, email: user.email, mobile: user.mobile, role: user.role };
  }

  saveEdit(user: UserProfile): void {
    if (this.editDraft.role && !this.roleService.canManage(this.auth.role(), this.editDraft.role)) {
      this.error.set('You cannot assign a role at or above your own.');
      return;
    }
    this.api.updateUser(user.id, this.editDraft).subscribe({
      next: (updated) => {
        this.managedUsers.update((list) => list.map((item) => item.id === user.id ? updated : item));
        this.editingId.set(null);
        this.message.set('Profile updated.');
        this.error.set('');
      },
      error: (error: HttpErrorResponse) => this.error.set(error.status
        ? `Could not update this profile (${error.status}).`
        : 'Could not reach the profiles API.'),
    });
  }

  toggleActive(user: UserProfile): void {
    this.api.setUserActive(user.id, !user.active).subscribe({
      next: (updated) => {
        this.managedUsers.update((list) => list.map((item) => item.id === user.id ? updated : item));
        this.message.set(updated.active ? 'Profile reactivated.' : 'Profile deactivated.');
        this.error.set('');
      },
      error: (error: HttpErrorResponse) => this.error.set(error.status
        ? `Could not change profile status (${error.status}).`
        : 'Could not reach the profiles API.'),
    });
  }

  deleteUser(user: UserProfile): void {
    if (!this.canManageProfile(user) || !globalThis.confirm(`Permanently delete ${user.name}'s profile? Related password-reset requests and verification codes will also be removed.`)) {
      return;
    }
    this.error.set('');
    this.message.set('');
    this.api.deleteUser(user.id).subscribe({
      next: () => {
        this.managedUsers.update((users) => users.filter((item) => item.id !== user.id));
        this.profilesLoaded.update((count) => Math.max(0, count - 1));
        this.totalProfiles.update((count) => Math.max(0, count - 1));
        this.message.set(`${user.name}'s profile was permanently deleted.`);
      },
      error: (error: HttpErrorResponse) => this.error.set(error.status
        ? `Could not delete this profile (${error.status}).`
        : 'Could not reach the profiles API.'),
    });
  }

  loadPendingResets(): void {
    this.api.listPendingPasswordResets().subscribe({
      next: (requests) => this.pendingResets.set(requests),
      error: (error: HttpErrorResponse) => this.error.set(error.status
        ? `Could not load pending password resets (${error.status}).`
        : 'Could not reach the password approval service.'),
    });
  }

  reviewReset(request: PasswordResetApproval, approve: boolean): void {
    this.api.reviewPasswordReset(request.id, approve).subscribe({
      next: () => {
        this.pendingResets.update((requests) => requests.filter((item) => item.id !== request.id));
        this.message.set(approve ? `Password reset approved for ${request.targetName}.` : `Password reset rejected for ${request.targetName}.`);
        this.error.set('');
      },
      error: (error: HttpErrorResponse) => this.error.set(error.status
        ? `Could not review the password reset (${error.status}).`
        : 'Could not reach the password approval service.'),
    });
  }
}
