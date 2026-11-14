import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { UserDirectoryDraft, UserDirectoryEntry, UserDirectoryService } from '../../core/services/user-directory.service';

const emptyDraft = (): UserDirectoryDraft => ({
  name: '',
  contactNumber: '',
  email: '',
  address: '',
  referredBy: '',
  serviceProvided: '',
});

@Component({
  selector: 'app-user-directory',
  standalone: true,
  imports: [FormsModule],
  styles: [`
    .table-scroll { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; min-width: 760px; }
    th, td { padding: .8rem; border-bottom: 1px solid var(--line); text-align: left; vertical-align: top; }
    th { color: var(--muted); font-size: .75rem; }
    td { color: var(--ink); font-size: .85rem; }
  `],
  template: `
    <section class="admin-page">
      <div class="admin-heading">
        <div><p class="eyebrow">Customer records</p><h1>User directory</h1><p>Customer accounts are synced here automatically. Add or review directory details below.</p></div>
        <span class="admin-count">{{ entries().length }} records</span>
      </div>
      @if (error()) { <div class="notice notice--error" role="alert">{{ error() }}</div> }
      @if (message()) { <div class="notice notice--success" role="status">{{ message() }}</div> }

      <form class="form-card" (ngSubmit)="create()" #directoryForm="ngForm">
        <p class="eyebrow">Manual entry</p><h2>Add a customer record</h2>
        <div class="form-row">
          <label>Name<input name="name" [(ngModel)]="draft.name" required maxlength="120"></label>
          <label>Contact number<input name="contactNumber" [(ngModel)]="draft.contactNumber" required maxlength="30" type="tel"></label>
        </div>
        <div class="form-row">
          <label>Email<input name="email" [(ngModel)]="draft.email" required maxlength="254" type="email"></label>
          <label>Referred by<input name="referredBy" [(ngModel)]="draft.referredBy" required maxlength="120"></label>
        </div>
        <label>Address<input name="address" [(ngModel)]="draft.address" required maxlength="300"></label>
        <label>Service provided<input name="serviceProvided" [(ngModel)]="draft.serviceProvided" required maxlength="160"></label>
        <button class="button" type="submit" [disabled]="directoryForm.invalid || saving()">
          {{ saving() ? 'Saving…' : 'Add to directory' }}
        </button>
      </form>

      <section class="user-list-panel">
        <div class="list-heading"><div><p class="eyebrow">Directory</p><h2>Customer records</h2></div>
          <label>Search records<input type="search" name="directorySearch" [ngModel]="search()" (ngModelChange)="search.set($event)" placeholder="Name, contact, email, address…"></label>
        </div>
        <div class="table-scroll">
          <table>
            <thead><tr><th>Name</th><th>Contact</th><th>Email</th><th>Address</th><th>Referred by</th><th>Service provided</th></tr></thead>
            <tbody>
              @for (entry of filteredEntries(); track entry.id) {
                <tr>
                  <td>{{ entry.name }}</td><td>{{ entry.contactNumber || '—' }}</td><td>{{ entry.email }}</td>
                  <td>{{ entry.address || '—' }}</td><td>{{ entry.referredBy || '—' }}</td><td>{{ entry.serviceProvided || '—' }}</td>
                </tr>
              } @empty {
                <tr><td colspan="6">{{ loading() ? 'Loading directory…' : 'No matching directory records.' }}</td></tr>
              }
            </tbody>
          </table>
        </div>
      </section>
    </section>
  `,
})
export class UserDirectory {
  private readonly directory = inject(UserDirectoryService);
  readonly entries = signal<UserDirectoryEntry[]>([]);
  readonly filteredEntries = computed(() => {
    const query = this.search().trim().toLowerCase();
    if (!query) return this.entries();
    return this.entries().filter((entry) => [
      entry.name, entry.contactNumber, entry.email, entry.address, entry.referredBy, entry.serviceProvided,
    ].some((value) => value.toLowerCase().includes(query)));
  });
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly search = signal('');
  draft = emptyDraft();

  constructor() {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.directory.list().subscribe({
      next: (entries) => {
        this.entries.set(entries);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.status
          ? `Could not load the user directory (${error.status}).`
          : 'Could not reach the user directory API.');
        this.loading.set(false);
      },
    });
  }

  create(): void {
    this.error.set('');
    this.message.set('');
    this.saving.set(true);
    const entry = Object.fromEntries(
      Object.entries(this.draft).map(([key, value]) => [key, value.trim()]),
    ) as UserDirectoryDraft;
    this.directory.create(entry).subscribe({
      next: (created) => {
        this.entries.update((current) => [...current, created].sort((a, b) => a.name.localeCompare(b.name)));
        this.draft = emptyDraft();
        this.message.set('Customer record added to the directory.');
        this.saving.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.status === 409
          ? 'A directory record already uses that email address.'
          : error.status
            ? `Could not save this directory record (${error.status}).`
            : 'Could not reach the user directory API.');
        this.saving.set(false);
      },
    });
  }
}
