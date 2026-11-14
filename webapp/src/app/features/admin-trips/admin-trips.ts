import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Trip, TripDraft } from '../../core/models/trip.model';
import { TripService } from '../../core/services/trip.service';
import { isPublicHttpUrl } from '../../core/services/image-url.service';

const emptyDraft = (): TripDraft => ({
  title: '',
  destination: '',
  summary: '',
  imageUrl: '',
  startDate: '',
  durationDays: 1,
  price: 0,
  category: 'Pilgrimage',
  featured: false,
});

@Component({
  selector: 'app-admin-trips',
  standalone: true,
  imports: [FormsModule, CurrencyPipe, DatePipe],
  template: `
    <section class="admin-page">
      <div class="admin-heading"><div><p class="eyebrow">Staff workspace</p><h1>Manage journeys</h1><p>Add new trips or update the details visitors see on the website.</p></div><span class="admin-count">{{ trips.trips().length }} journeys</span></div>
      <div class="admin-layout">
        <form class="form-card trip-editor" (ngSubmit)="save()" #tripForm="ngForm">
          <div class="editor-heading"><div><p class="eyebrow">{{ editingId() ? 'Update listing' : 'Create a listing' }}</p><h2>{{ editingId() ? 'Edit journey' : 'Add a journey' }}</h2></div>@if (editingId()) { <button type="button" class="text-button" (click)="reset()">Cancel edit</button> }</div>
          @if (message()) { <div class="notice notice--success" role="status">{{ message() }}</div> }
          @if (error()) { <div class="notice notice--error" role="alert">{{ error() }}</div> }
          <label>Journey title<input name="title" [(ngModel)]="draft.title" required maxlength="80" placeholder="e.g. Varanasi Spiritual Journey"></label>
          <label>Destination<input name="destination" [(ngModel)]="draft.destination" required maxlength="100" placeholder="City, state"></label>
          <label>Short description<textarea name="summary" [(ngModel)]="draft.summary" required maxlength="260" rows="3" placeholder="What makes this journey special?"></textarea></label>
          <label>Cover image URL<input name="imageUrl" [(ngModel)]="draft.imageUrl" (ngModelChange)="imagePreviewFailed.set(false)" required type="text" placeholder="https://…"></label>
          <div class="gallery-preview" [class.gallery-preview--empty]="!draft.imageUrl">
            @if (draft.imageUrl) { <img [src]="draft.imageUrl" [alt]="draft.destination || 'Journey image preview'" (error)="imagePreviewFailed.set(true)"> @if (imagePreviewFailed()) { <span>Image preview could not be loaded. Check the URL.</span> } }
            @else { <span>Image preview will appear here</span> }
          </div>
          <p class="form-hint">Provide a public HTTP or HTTPS image URL. Only the URL is saved in the trips database.</p>
          <div class="form-row"><label>Departure date<input name="startDate" [(ngModel)]="draft.startDate" required type="date"></label><label>Duration (days)<input name="durationDays" [(ngModel)]="draft.durationDays" required type="number" min="1" max="60"></label></div>
          <div class="form-row"><label>Price per person (₹)<input name="price" [(ngModel)]="draft.price" required type="number" min="0"></label><label>Category<select name="category" [(ngModel)]="draft.category"><option>Pilgrimage</option><option>Heritage</option><option>Nature</option></select></label></div>
          <label class="checkbox-label"><input name="featured" [(ngModel)]="draft.featured" type="checkbox"> Feature this journey on the home page</label>
          <button class="button" type="submit" [disabled]="tripForm.invalid || saving()">{{ saving() ? 'Saving…' : editingId() ? 'Save changes' : 'Add journey' }} <span aria-hidden="true">→</span></button>
          <p class="form-hint">Journey changes are saved in the trips database.</p>
        </form>

        <div class="trip-list-panel"><div class="list-heading"><div><p class="eyebrow">Published journeys</p><h2>Current listings</h2></div></div>
          @for (trip of trips.trips(); track trip.id) {
            <article class="admin-trip">
              <img [src]="trip.imageUrl" [alt]="trip.destination" loading="lazy">
              <div class="admin-trip__info"><span class="eyebrow">{{ trip.category }} · {{ trip.destination }}</span><h3>{{ trip.title }}</h3><p>{{ trip.durationDays }} days · {{ trip.price | currency:'INR':'symbol':'1.0-0' }} · {{ trip.startDate | date:'mediumDate' }}</p><div class="admin-trip__actions"><button class="text-button" type="button" (click)="edit(trip)">Edit</button><button class="text-button text-button--danger" type="button" (click)="remove(trip)">Delete</button></div></div>
            </article>
          } @empty { <div class="empty-state"><h3>No journeys listed</h3><p>Add a journey using the form to get started.</p></div> }
        </div>
      </div>
    </section>
  `,
})
export class AdminTrips {
  readonly editingId = signal<string | null>(null);
  readonly message = signal('');
  readonly error = signal('');
  readonly imagePreviewFailed = signal(false);
  readonly saving = signal(false);
  draft: TripDraft = emptyDraft();

  constructor(readonly trips: TripService) {}

  save(): void {
    this.message.set('');
    this.error.set('');
    if (!isPublicHttpUrl(this.draft.imageUrl)) {
      this.error.set('Enter a public HTTP or HTTPS image URL.');
      return;
    }
    const draft = { ...this.draft, imageUrl: this.draft.imageUrl.trim() };
    const id = this.editingId();
    this.saving.set(true);
    const request = id ? this.trips.updateTrip(id, draft) : this.trips.addTrip(draft);
    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.message.set(id ? 'Journey updated successfully.' : 'Journey added successfully.');
        this.imagePreviewFailed.set(false);
        this.reset();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.error.set(error instanceof Error ? error.message : 'Could not save this journey.');
      },
    });
  }

  edit(trip: Trip): void {
    this.editingId.set(trip.id);
    this.draft = {
      title: trip.title,
      destination: trip.destination,
      summary: trip.summary,
      imageUrl: trip.imageUrl,
      startDate: trip.startDate,
      durationDays: trip.durationDays,
      price: trip.price,
      category: trip.category,
      featured: trip.featured,
    };
    this.message.set('');
    this.error.set('');
  }

  remove(trip: Trip): void {
    if (!globalThis.confirm(`Delete "${trip.title}"? This cannot be undone.`)) return;
    this.trips.deleteTrip(trip.id).subscribe({
      next: () => {
        if (this.editingId() === trip.id) this.reset();
        this.message.set(`"${trip.title}" was deleted.`);
        this.error.set('');
      },
      error: (error: unknown) => this.error.set(error instanceof Error ? error.message : 'Could not delete this journey.'),
    });
  }

  reset(): void {
    this.draft = emptyDraft();
    this.editingId.set(null);
    this.imagePreviewFailed.set(false);
  }
}
