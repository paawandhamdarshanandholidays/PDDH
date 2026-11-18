import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { UserDirectoryDraft, UserDirectoryEntry, UserDirectoryService } from '../../core/services/user-directory.service';

type DirectorySort = 'name' | 'email' | 'id';
type DirectoryAction = 'view' | 'add' | null;

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
    .table-scroll { overflow-x: auto; border: 1px solid var(--line); border-radius: 1rem; background: #fff; }
    table { width: 100%; border-collapse: collapse; min-width: 1040px; }
    th, td { padding: .8rem 1rem; border-bottom: 1px solid var(--line); text-align: left; vertical-align: top; }
    th { color: var(--muted); background: #f8fafc; font-size: .72rem; letter-spacing: .06em; text-transform: uppercase; }
    th button { padding: 0; border: 0; background: none; color: inherit; font: inherit; letter-spacing: inherit; text-transform: inherit; cursor: pointer; }
    td { color: var(--ink); font-size: .85rem; }
    tbody tr:hover { background: #f8fafc; }
    .directory-toolbar, .directory-pager { display: flex; align-items: end; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
    .directory-toolbar label { min-width: 220px; }
    .directory-pager { padding: .9rem 0; color: var(--muted); font-size: .85rem; }
    .directory-pager__actions { display: flex; align-items: center; gap: .6rem; }
    .directory-choice { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 1rem; }
    .directory-choice button { min-height: 112px; padding: 1.25rem; border: 1px solid var(--line); border-radius: 1rem; background: linear-gradient(135deg, #fff, #f8fafc); color: var(--ink); text-align: left; cursor: pointer; }
    .directory-choice button:hover { border-color: var(--accent, #b37a35); box-shadow: 0 8px 24px rgb(15 23 42 / 8%); }
    .directory-choice strong { display: block; margin-bottom: .35rem; font-size: 1rem; }
    .directory-choice span { color: var(--muted); font-size: .84rem; }
  `],
  template: `
    <section class="admin-page">
      <div class="admin-heading">
        <div><p class="eyebrow">Customer records</p><h1>User directory</h1>
          <p>Browse registered customer accounts and manually entered records. Registered accounts are read-only here; manual records can be edited or deleted.</p>
        </div>
        @if (action()) { <button class="text-button" type="button" (click)="returnToChoices()">Back to options</button> }
      </div>
      @if (error()) { <div class="notice notice--error" role="alert">{{ error() }}</div> }
      @if (message()) { <div class="notice notice--success" role="status">{{ message() }}</div> }

      @if (!action()) {
        <div class="directory-choice" aria-label="User directory options">
          <button type="button" (click)="openDirectory()"><strong>View user directory</strong><span>Browse, search, sort, and page through saved customer records.</span></button>
          <button type="button" (click)="openAddForm()"><strong>Add a customer record</strong><span>Save contact and service details for a customer.</span></button>
        </div>
      }

      @if (action() === 'add') {
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
            {{ saving() ? 'Saving…' : 'Save customer record' }}
          </button>
        </form>
      }

      @if (action() === 'view') {
        <section class="user-list-panel">
          <div class="list-heading directory-toolbar">
            <div><p class="eyebrow">Saved customers</p><h2>User directory <span class="admin-count">{{ total() }}</span></h2></div>
            <label>Search this page<input type="search" name="directorySearch" [ngModel]="search()" (ngModelChange)="search.set($event)" placeholder="Name, contact, email, address…"></label>
          </div>
          @if (loading()) { <p class="form-hint" role="status">Loading this directory page…</p> }
          <div class="table-scroll">
            <table>
              <thead><tr>
                <th><button type="button" (click)="sortBy('name')">Name {{ sortMark('name') }}</button></th>
                <th>Contact</th><th><button type="button" (click)="sortBy('email')">Email {{ sortMark('email') }}</button></th>
                <th>Address</th><th>Referred by</th><th>Service provided</th><th>Record type</th><th>Actions</th>
              </tr></thead>
              <tbody>
                @for (entry of filteredEntries(); track entry.id) {
                  @if (editingId() === entry.id) {
                    <tr><td colspan="8">
                      <form class="form-card" (ngSubmit)="saveEdit(entry)" #editForm="ngForm">
                        <p class="eyebrow">Manual entry</p><h3>Edit {{ entry.name }}</h3>
                        <div class="form-row">
                          <label>Name<input name="editName" [(ngModel)]="editDraft.name" required maxlength="120"></label>
                          <label>Contact number<input name="editContact" [(ngModel)]="editDraft.contactNumber" required maxlength="30" type="tel"></label>
                        </div>
                        <div class="form-row">
                          <label>Email<input name="editEmail" [(ngModel)]="editDraft.email" required maxlength="254" type="email"></label>
                          <label>Referred by<input name="editReferredBy" [(ngModel)]="editDraft.referredBy" required maxlength="120"></label>
                        </div>
                        <label>Address<input name="editAddress" [(ngModel)]="editDraft.address" required maxlength="300"></label>
                        <label>Service provided<input name="editService" [(ngModel)]="editDraft.serviceProvided" required maxlength="160"></label>
                        <div class="user-card__actions">
                          <button class="button button--small" type="submit" [disabled]="editForm.invalid || saving()">Save changes</button>
                          <button class="text-button" type="button" (click)="editingId.set(null)" [disabled]="saving()">Cancel</button>
                        </div>
                      </form>
                    </td></tr>
                  } @else {
                    <tr>
                      <td>{{ entry.name }}</td><td>{{ entry.contactNumber || '—' }}</td><td>{{ entry.email }}</td>
                      <td>{{ entry.address || '—' }}</td><td>{{ entry.referredBy || '—' }}</td>
                      <td>{{ entry.serviceProvided || '—' }}</td>
                      <td>{{ entry.source === 'ACCOUNT' ? 'Registered account' : 'Manual entry' }}</td>
                      <td>
                        @if (entry.source === 'MANUAL') {
                          <button class="text-button" type="button" (click)="edit(entry)" [disabled]="saving()">Edit</button>
                          <button class="text-button text-button--danger" type="button" (click)="delete(entry)" [disabled]="saving()">Delete</button>
                        } @else { <span class="form-hint">Read only</span> }
                      </td>
                    </tr>
                  }
                } @empty {
                  <tr><td colspan="8">{{ loading() ? 'Loading directory…' : 'No records match the loaded page and search.' }}</td></tr>
                }
              </tbody>
            </table>
          </div>
          <div class="directory-pager">
            <label>Rows per page
              <select name="pageSize" [ngModel]="size()" (ngModelChange)="setPageSize($event)">
                @for (option of pageSizes; track option) { <option [ngValue]="option">{{ option }}</option> }
              </select>
            </label>
            <div class="directory-pager__actions"><span>Page {{ page() + 1 }} of {{ pageCount() }} · {{ total() }} total</span>
              <button class="button button--small" type="button" (click)="changePage(-1)" [disabled]="page() === 0 || loading()">Previous</button>
              <button class="button button--small" type="button" (click)="changePage(1)" [disabled]="page() + 1 >= totalPages() || loading()">Next</button>
            </div>
          </div>
        </section>
      }
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
  readonly action = signal<DirectoryAction>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly search = signal('');
  readonly page = signal(0);
  readonly size = signal(50);
  readonly total = signal(0);
  readonly totalPages = signal(0);
  readonly sort = signal<DirectorySort>('name');
  readonly direction = signal<'ASC' | 'DESC'>('ASC');
  readonly pageSizes = [10, 25, 50];
  draft = emptyDraft();
  readonly editingId = signal<string | null>(null);
  editDraft = emptyDraft();
  private readonly requestId = signal(0);

  openDirectory(): void {
    this.error.set('');
    this.message.set('');
    this.action.set('view');
    this.page.set(0);
    this.load();
  }

  openAddForm(): void {
    this.error.set('');
    this.message.set('');
    this.action.set('add');
    this.draft = emptyDraft();
  }

  returnToChoices(): void {
    this.requestId.update((current) => current + 1);
    this.action.set(null);
    this.loading.set(false);
    this.error.set('');
  }

  pageCount(): number {
    return Math.max(this.totalPages(), 1);
  }

  sortMark(field: DirectorySort): string {
    return this.sort() === field ? (this.direction() === 'ASC' ? '↑' : '↓') : '↕';
  }

  setPageSize(size: number): void {
    if (!this.pageSizes.includes(Number(size))) return;
    this.size.set(Number(size));
    this.page.set(0);
    this.load();
  }

  changePage(delta: number): void {
    const page = this.page() + delta;
    if (page < 0 || page >= this.totalPages()) return;
    this.page.set(page);
    this.load();
  }

  sortBy(field: DirectorySort): void {
    this.direction.set(this.sort() === field && this.direction() === 'ASC' ? 'DESC' : 'ASC');
    this.sort.set(field);
    this.page.set(0);
    this.load();
  }

  load(): void {
    const requestId = this.requestId() + 1;
    this.requestId.set(requestId);
    this.loading.set(true);
    this.error.set('');
    this.directory.listPage(this.page(), this.size(), this.sort(), this.direction()).subscribe({
      next: (response) => {
        if (requestId !== this.requestId() || this.action() !== 'view') return;
        this.entries.set(response.content);
        this.total.set(response.totalElements);
        this.totalPages.set(response.totalPages);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        if (requestId !== this.requestId() || this.action() !== 'view') return;
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
      next: () => {
        this.draft = emptyDraft();
        this.saving.set(false);
        this.message.set('Customer record saved to the directory.');
        this.action.set('view');
        this.page.set(0);
        this.load();
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

  edit(entry: UserDirectoryEntry): void {
    if (entry.source !== 'MANUAL') return;
    this.error.set('');
    this.message.set('');
    this.editingId.set(entry.id);
    this.editDraft = {
      name: entry.name,
      contactNumber: entry.contactNumber,
      email: entry.email,
      address: entry.address,
      referredBy: entry.referredBy,
      serviceProvided: entry.serviceProvided,
    };
  }

  saveEdit(entry: UserDirectoryEntry): void {
    if (entry.source !== 'MANUAL' || this.editingId() !== entry.id) return;
    this.error.set('');
    this.message.set('');
    this.saving.set(true);
    const update = Object.fromEntries(
      Object.entries(this.editDraft).map(([key, value]) => [key, value.trim()]),
    ) as UserDirectoryDraft;
    this.directory.update(entry.id, update).subscribe({
      next: (updated) => {
        this.entries.update((items) => items.map((item) => item.id === entry.id ? updated : item));
        this.editingId.set(null);
        this.saving.set(false);
        this.message.set('Manual customer record updated.');
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.status
          ? `Could not update this manual record (${error.status}).`
          : 'Could not reach the user directory API.');
        this.saving.set(false);
      },
    });
  }

  delete(entry: UserDirectoryEntry): void {
    if (entry.source !== 'MANUAL'
        || !globalThis.confirm(`Permanently delete the manual record for ${entry.name}?`)) return;
    this.error.set('');
    this.message.set('');
    this.saving.set(true);
    this.directory.delete(entry.id).subscribe({
      next: () => {
        this.entries.update((items) => items.filter((item) => item.id !== entry.id));
        this.total.update((count) => Math.max(0, count - 1));
        this.totalPages.set(Math.ceil(this.total() / this.size()));
        this.saving.set(false);
        this.message.set('Manual customer record deleted.');
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.status
          ? `Could not delete this manual record (${error.status}).`
          : 'Could not reach the user directory API.');
        this.saving.set(false);
      },
    });
  }
}
