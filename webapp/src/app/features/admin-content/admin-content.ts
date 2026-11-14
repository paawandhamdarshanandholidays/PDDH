import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { GalleryImage } from '../../core/models/content.model';
import { Destination, Event as ContentEvent } from '../../core/models/content.model';
import { Hotel, HotelDraft } from '../../core/models/hotel.model';
import { OfferDraft } from '../../core/models/offer.model';
import { ContentService } from '../../core/services/content.service';
import { GalleryService } from '../../core/services/gallery.service';
import { HotelService } from '../../core/services/hotel.service';
import { OfferService } from '../../core/services/offer.service';
import { TripService } from '../../core/services/trip.service';
import { ContactDetailsDraft, ContactDetailsService } from '../../core/services/contact-details.service';
import { ImageUrlService, isImageSource } from '../../core/services/image-url.service';
import { ImageSourceField } from '../../shared/components/image-source-field/image-source-field';

const emptyOffer = (): OfferDraft => ({
  title: '',
  description: '',
  code: '',
  discountPercent: 10,
  validUntil: '',
  tripId: '',
  active: true,
  imageUrl: '',
});

@Component({
  selector: 'app-admin-content',
  standalone: true,
  imports: [FormsModule, CurrencyPipe, DatePipe, RouterLink, ImageSourceField],
  template: `
    <section class="admin-page">
      <div class="admin-heading"><div><p class="eyebrow">Staff workspace</p><h1>Offers & gallery</h1><p>Publish offers and add journey images for visitors to explore.</p></div><div class="admin-heading__links"><a class="text-link" routerLink="/admin/trips">Manage trips <span aria-hidden="true">→</span></a><span class="admin-count">{{ gallery.images().length }} images · {{ offers.offers().length }} offers</span></div></div>
      <nav class="content-form-nav" aria-label="Choose website content form">
        <button type="button" [class.content-form-nav__item--active]="selectedForm() === 'temple'" [attr.aria-pressed]="selectedForm() === 'temple'" (click)="selectForm('temple')">Add Temple</button>
        <button type="button" [class.content-form-nav__item--active]="selectedForm() === 'place'" [attr.aria-pressed]="selectedForm() === 'place'" (click)="selectForm('place')">Add Places</button>
        <button type="button" [class.content-form-nav__item--active]="selectedForm() === 'hotel'" [attr.aria-pressed]="selectedForm() === 'hotel'" (click)="selectForm('hotel')">Add Accommodation</button>
        <button type="button" [class.content-form-nav__item--active]="selectedForm() === 'event'" [attr.aria-pressed]="selectedForm() === 'event'" (click)="selectForm('event')">Add Event</button>
        <button type="button" [class.content-form-nav__item--active]="selectedForm() === 'gallery'" [attr.aria-pressed]="selectedForm() === 'gallery'" (click)="selectForm('gallery')">Add Gallery Image</button>
        <button type="button" [class.content-form-nav__item--active]="selectedForm() === 'offer'" [attr.aria-pressed]="selectedForm() === 'offer'" (click)="selectForm('offer')">Add Offer</button>
        <button type="button" [class.content-form-nav__item--active]="selectedForm() === 'contact'" [attr.aria-pressed]="selectedForm() === 'contact'" (click)="selectForm('contact')">Contact Details</button>
      </nav>
      <div class="content-manager">
@if (selectedForm() === 'temple' || selectedForm() === 'place') {
        <section class="manager-section">
          <form class="form-card manager-form" (ngSubmit)="addDestination()" #destinationForm="ngForm">
            <div><p class="eyebrow">Manage places of interest</p><h2>{{ selectedForm() === 'temple' ? 'Add a temple' : 'Add a place' }}</h2></div>
            @if (destinationMessage()) { <div class="notice notice--success" role="status">{{ destinationMessage() }}</div> }
            @if (destinationError()) { <div class="notice notice--error" role="alert">{{ destinationError() }}</div> }
            <label>Name<input name="destinationName" [(ngModel)]="destinationDraft.name" required maxlength="80"></label>
            <label>Location<input name="region" [(ngModel)]="destinationDraft.region" required maxlength="100" placeholder="City, state"></label>
            <label>Description<textarea name="destinationDescription" [(ngModel)]="destinationDraft.description" required maxlength="260" rows="3"></textarea></label>
            <app-image-source-field name="destinationImage" [(ngModel)]="destinationDraft.imageUrl" (urlBlur)="resolveDestinationImage()"
              [label]="selectedForm() === 'temple' ? 'Temple image' : 'Place image'" [altText]="destinationDraft.name || 'Destination image preview'" required />
            @if (resolvingDestinationImage()) { <p class="form-hint" role="status">Preparing image preview…</p> }
            <label>Category<select name="destinationCategory" [(ngModel)]="destinationDraft.category" [disabled]="destinationDraft.kind === 'Temple'"><option value="Temple">Temple</option><option value="Nature">Nature</option><option value="Heritage">Heritage</option></select></label>
            <button class="button" type="submit" [disabled]="destinationForm.invalid || resolvingDestinationImage()">Add {{ selectedForm() }}</button>
            <p class="form-hint">Paste a public image URL or upload a file. Unsplash photo pages are also supported.</p>
          </form>
          <div class="manager-list">
            <div class="list-heading"><div><p class="eyebrow">Public destination pages</p><h2>{{ selectedForm() === 'temple' ? 'Temples' : 'Places' }}</h2></div></div>
            <div class="admin-record-grid">
              @if (selectedForm() === 'temple') { @for (place of content.temples(); track place.id) { <article class="admin-record"><img [src]="place.imageUrl" [alt]="place.name"><div><span>{{ place.kind }}</span><h3>{{ place.name }}</h3><button class="text-button text-button--danger" type="button" (click)="removeDestination(place)">Remove</button></div></article> } }
              @if (selectedForm() === 'place') { @for (place of content.places(); track place.id) { <article class="admin-record"><img [src]="place.imageUrl" [alt]="place.name"><div><span>{{ place.kind }} · {{ place.category }}</span><h3>{{ place.name }}</h3><button class="text-button text-button--danger" type="button" (click)="removeDestination(place)">Remove</button></div></article> } }
            </div>
          </div>
        </section>
        }

@if (selectedForm() === 'hotel') {
        <section class="manager-section manager-section--offers">
          <form class="form-card manager-form" (ngSubmit)="addHotel()" #hotelForm="ngForm">
            <div><p class="eyebrow">Offer a place to rest</p><h2>Add accommodation</h2></div>
            @if (hotelMessage()) { <div class="notice notice--success" role="status">{{ hotelMessage() }}</div> }
            @if (hotelError()) { <div class="notice notice--error" role="alert">{{ hotelError() }}</div> }
            <label>Hotel or dharamshala name<input name="hotelName" [(ngModel)]="hotelDraft.name" required maxlength="90"></label>
            <label>Location<input name="hotelLocation" [(ngModel)]="hotelDraft.location" required maxlength="100"></label>
            <label>Description<textarea name="hotelDescription" [(ngModel)]="hotelDraft.description" required maxlength="240" rows="3"></textarea></label>
            <app-image-source-field name="hotelImage" [(ngModel)]="hotelDraft.imageUrl" label="Accommodation image"
              [altText]="hotelDraft.name || 'Accommodation image preview'" required />
            <label>Price per night (₹)<input name="pricePerNight" [(ngModel)]="hotelDraft.pricePerNight" required type="number" min="0"></label>
            <label>Amenities<input name="amenities" [(ngModel)]="hotelDraft.amenities" required maxlength="180" placeholder="Family rooms, Wi-Fi, Parking"></label>
            <button class="button" type="submit" [disabled]="hotelForm.invalid">Publish accommodation</button>
            <p class="form-hint">Paste a public image URL or upload an image to the stays database.</p>
          </form>
          <div class="manager-list">
            <div class="list-heading"><div><p class="eyebrow">Available stays</p><h2>Hotels & dharamshalas</h2></div></div>
            <div class="admin-offer-list">@for (hotel of hotels.hotels(); track hotel.id) { <article class="admin-offer"><img class="admin-hotel-image" [src]="hotel.imageUrl" [alt]="hotel.name"><div class="admin-offer__body"><h3>{{ hotel.name }}</h3><p>{{ hotel.location }} · {{ hotel.pricePerNight | currency:'INR':'symbol':'1.0-0' }}/night</p><span>{{ hotel.amenities }}</span></div><button class="text-button text-button--danger" type="button" (click)="removeHotel(hotel.id)">Remove</button></article> } @empty { <div class="empty-state"><h3>No stays listed</h3><p>Add a hotel or dharamshala for travellers.</p></div> }</div>
          </div>
        </section>
        }

@if (selectedForm() === 'event') {
        <section class="manager-section manager-section--offers">
          <form class="form-card manager-form" (ngSubmit)="addEvent()" #eventForm="ngForm">
            <div><p class="eyebrow">Keep the calendar current</p><h2>Add an event</h2></div>
            @if (eventMessage()) { <div class="notice notice--success" role="status">{{ eventMessage() }}</div> }
            @if (eventError()) { <div class="notice notice--error" role="alert">{{ eventError() }}</div> }
            <label>Event title<input name="eventTitle" [(ngModel)]="eventDraft.title" required maxlength="90"></label>
            <div class="form-row"><label>Date or schedule<input name="eventDate" [(ngModel)]="eventDraft.date" required maxlength="60"></label><label>Location<input name="eventLocation" [(ngModel)]="eventDraft.location" required maxlength="90"></label></div>
            <label>Description<textarea name="eventDescription" [(ngModel)]="eventDraft.description" required maxlength="240" rows="3"></textarea></label>
            <app-image-source-field name="eventImage" [(ngModel)]="eventDraft.imageUrl" label="Event image"
              [altText]="eventDraft.title || 'Event image preview'" required />
            <button class="button" type="submit" [disabled]="eventForm.invalid">Publish event</button>
            <p class="form-hint">Paste a public image URL or upload an image to the events database.</p>
          </form>
          <div class="manager-list">
            <div class="list-heading"><div><p class="eyebrow">Public event page</p><h2>Current events</h2></div></div>
            <div class="admin-offer-list">@for (event of content.events(); track event.id) { <article class="admin-offer"><img class="admin-hotel-image" [src]="event.imageUrl" [alt]="event.title"><div class="admin-offer__body"><h3>{{ event.title }}</h3><p>{{ event.date }} · {{ event.location }}</p><span>{{ event.description }}</span></div><button class="text-button text-button--danger" type="button" (click)="removeEvent(event.id)">Remove</button></article> }</div>
          </div>
        </section>
        }

@if (selectedForm() === 'gallery') {
        <section class="manager-section">
          <form class="form-card manager-form" (ngSubmit)="addImage()" #imageForm="ngForm">
            <div><p class="eyebrow">Build your visual story</p><h2>Add a gallery image</h2></div>
            @if (imageMessage()) { <div class="notice notice--success" role="status">{{ imageMessage() }}</div> }
            @if (imageError()) { <div class="notice notice--error" role="alert">{{ imageError() }}</div> }
            <app-image-source-field name="galleryImage" [(ngModel)]="imageDraft.src" label="Gallery image"
              altText="Preview of the image to add" required />
            <label>Image description<input name="alt" [(ngModel)]="imageDraft.alt" required maxlength="120" placeholder="Describe the scene for visitors"></label>
            <label>Gallery category<input name="category" [(ngModel)]="imageDraft.category" required maxlength="40" placeholder="Varanasi, Ayodhya, Nature..."></label>
            <button class="button" type="submit" [disabled]="imageForm.invalid || !imageDraft.src">Add image to gallery <span aria-hidden="true">→</span></button>
            <p class="form-hint">Paste a public image URL or upload an image to the gallery database.</p>
          </form>
          <div class="manager-list">
            <div class="list-heading"><div><p class="eyebrow">Website gallery</p><h2>Image library</h2></div></div>
            <div class="admin-gallery-grid">
              @for (image of gallery.images(); track image.id) {
                <article class="admin-gallery-tile"><img [src]="image.src" [alt]="image.alt" loading="lazy"><div><span>{{ image.category }}</span><button class="text-button text-button--danger" type="button" (click)="removeImage(image)">Remove</button></div><p>{{ image.alt }}</p></article>
              }
            </div>
          </div>
        </section>
        }

@if (selectedForm() === 'offer') {
        <section class="manager-section manager-section--offers">
          <form class="form-card manager-form" (ngSubmit)="addOffer()" #offerForm="ngForm">
            <div><p class="eyebrow">Give travellers a reason to go</p><h2>Create an offer</h2></div>
            @if (offerMessage()) { <div class="notice notice--success" role="status">{{ offerMessage() }}</div> }
            @if (offerError()) { <div class="notice notice--error" role="alert">{{ offerError() }}</div> }
            <label>Offer title<input name="offerTitle" [(ngModel)]="offerDraft.title" required maxlength="80" placeholder="A little more time in Braj"></label>
            <label>Description<textarea name="offerDescription" [(ngModel)]="offerDraft.description" required maxlength="220" rows="3" placeholder="What does this offer include?"></textarea></label>
            <div class="form-row"><label>Promo code<input name="code" [(ngModel)]="offerDraft.code" required maxlength="20" placeholder="JOURNEY10"></label><label>Discount (%)<input name="discount" [(ngModel)]="offerDraft.discountPercent" required type="number" min="1" max="90"></label></div>
            <label>Applies to<select name="tripId" [(ngModel)]="offerDraft.tripId" required><option value="">Choose a journey</option>@for (trip of trips.trips(); track trip.id) { <option [value]="trip.id">{{ trip.title }}</option> }</select></label>
            <label>Valid until<input name="validUntil" [(ngModel)]="offerDraft.validUntil" [min]="today" required type="date"></label>
            <app-image-source-field name="offerImage" [(ngModel)]="offerDraft.imageUrl" label="Offer image"
              [altText]="offerDraft.title || 'Offer image preview'" required />
            <button class="button" type="submit" [disabled]="offerForm.invalid || !trips.trips().length">Publish offer <span aria-hidden="true">→</span></button>
            @if (!trips.trips().length) { <p class="form-hint">Add a trip before publishing a linked offer.</p> }
          </form>
          <div class="manager-list">
            <div class="list-heading"><div><p class="eyebrow">Manage promotions</p><h2>Current offers</h2></div></div>
            <div class="admin-offer-list">
              @for (offer of offers.offers(); track offer.id) {
                <article class="admin-offer">@if (offer.imageUrl) { <img class="admin-hotel-image" [src]="offer.imageUrl" [alt]="offer.title"> }<div class="admin-offer__discount">{{ offer.discountPercent }}%</div><div class="admin-offer__body"><h3>{{ offer.title }}</h3><p>{{ offer.code }} · until {{ offer.validUntil | date:'mediumDate' }}</p><span>{{ tripTitle(offer.tripId) }}</span></div><button class="text-button text-button--danger" type="button" (click)="removeOffer(offer.id)">Remove</button></article>
              } @empty { <div class="empty-state"><h3>No offers yet</h3><p>Use the form to publish your first offer.</p></div> }
            </div>
          </div>
        </section>
        }
@if (selectedForm() === 'contact') {
        <section class="manager-section">
          <form class="form-card manager-form" (ngSubmit)="addContactDetails()" #contactForm="ngForm">
            <div><p class="eyebrow">Keep public contact information current</p><h2>Add contact details</h2></div>
            @if (contactMessage()) { <div class="notice notice--success" role="status">{{ contactMessage() }}</div> }
            @if (contactError()) { <div class="notice notice--error" role="alert">{{ contactError() }}</div> }
            <label>Phone<input name="contactPhone" [(ngModel)]="contactDraft.phone" required maxlength="30" type="tel"></label>
            <label>Email<input name="contactEmail" [(ngModel)]="contactDraft.email" required maxlength="120" type="email"></label>
            <label>Office hours<input name="officeHours" [(ngModel)]="contactDraft.officeHours" required maxlength="120"></label>
            <button class="button" type="submit" [disabled]="contactForm.invalid">Save contact details</button>
          </form>
          <div class="manager-list">
            <div class="list-heading"><div><p class="eyebrow">Public contact page</p><h2>Contact details</h2></div></div>
            <div class="admin-offer-list">
              @for (details of contacts.items(); track details.id) {
                <article class="admin-offer"><div class="admin-offer__body"><h3>{{ details.phone }}</h3><p>{{ details.email }}</p><span>{{ details.officeHours }}</span></div><button class="text-button text-button--danger" type="button" (click)="removeContactDetails(details.id)">Remove</button></article>
              } @empty { <div class="empty-state"><h3>No contact details yet</h3><p>Add the phone, email, and office hours shown on the contact page.</p></div> }
            </div>
          </div>
        </section>
        }
      </div>
    </section>
  `,
})
export class AdminContent {
  readonly destinationMessage = signal('');
  readonly destinationError = signal('');
  readonly resolvingDestinationImage = signal(false);
  readonly hotelMessage = signal('');
  readonly hotelError = signal('');
  readonly eventMessage = signal('');
  readonly eventError = signal('');
  readonly imageMessage = signal('');
  readonly imageError = signal('');
  readonly offerMessage = signal('');
  readonly offerError = signal('');
  readonly contactMessage = signal('');
  readonly contactError = signal('');
  readonly today = new Date().toISOString().slice(0, 10);
  private readonly imageUrls = inject(ImageUrlService);
  readonly selectedForm = signal<'temple' | 'place' | 'hotel' | 'event' | 'gallery' | 'offer' | 'contact'>('temple');
  imageDraft: Omit<GalleryImage, 'id'> = { src: '', alt: '', category: '' };
  offerDraft = emptyOffer();
  destinationDraft: Omit<Destination, 'id'> = { kind: 'Temple', name: '', region: '', description: '', imageUrl: '', category: 'Temple' };
  hotelDraft: HotelDraft = { name: '', location: '', description: '', imageUrl: '', pricePerNight: 0, amenities: '' };
  eventDraft: Omit<ContentEvent, 'id'> = { title: '', date: '', location: '', description: '', imageUrl: '' };
  contactDraft: ContactDetailsDraft = { phone: '', email: '', officeHours: '' };

  constructor(
    readonly content: ContentService,
    readonly gallery: GalleryService,
    readonly hotels: HotelService,
    readonly offers: OfferService,
    readonly trips: TripService,
    readonly contacts: ContactDetailsService,
  ) {}

  selectForm(form: 'temple' | 'place' | 'hotel' | 'event' | 'gallery' | 'offer' | 'contact'): void {
    this.selectedForm.set(form);
    if (form === 'temple' || form === 'place') this.setDestinationKind(form === 'temple' ? 'Temple' : 'Place');
    this.destinationMessage.set('');
    this.destinationError.set('');
    this.hotelMessage.set('');
    this.hotelError.set('');
    this.eventMessage.set('');
    this.eventError.set('');
    this.imageMessage.set('');
    this.imageError.set('');
    this.offerMessage.set('');
    this.offerError.set('');
    this.contactMessage.set('');
    this.contactError.set('');
  }

  setDestinationKind(kind: Destination['kind']): void {
    if (kind === 'Temple') this.destinationDraft.category = 'Temple';
    else if (this.destinationDraft.category === 'Temple') this.destinationDraft.category = 'Heritage';
  }

  addDestination(): void {
    this.destinationError.set('');
    this.destinationMessage.set('');
    if (isUnsplashPhotoPage(this.destinationDraft.imageUrl)) {
      this.resolveDestinationImage(() => this.persistDestination());
      return;
    }
    if (!isImageSource(this.destinationDraft.imageUrl)) {
      this.destinationError.set('Enter a public image URL or choose a supported image file.');
      return;
    }
    this.persistDestination();
  }

  private persistDestination(): void {
    const kind: Destination['kind'] = this.selectedForm() === 'place' ? 'Place' : 'Temple';
    const draft = { ...this.destinationDraft, kind, name: this.destinationDraft.name.trim(), region: this.destinationDraft.region.trim(), description: this.destinationDraft.description.trim(), imageUrl: this.destinationDraft.imageUrl.trim() };
    this.content.addDestination(draft).subscribe({
      next: () => {
        this.destinationDraft = { kind, name: '', region: '', description: '', imageUrl: '', category: kind === 'Temple' ? 'Temple' : 'Heritage' };
        this.destinationMessage.set('The new listing is now on its public page.');
      },
      error: (error: unknown) => this.destinationError.set(writeError(error, 'Could not add this place')),
    });
  }

  resolveDestinationImage(afterResolve?: () => void): void {
    const url = this.destinationDraft.imageUrl.trim();
    if (!isUnsplashPhotoPage(url) || this.resolvingDestinationImage()) return;
    this.destinationError.set('');
    this.resolvingDestinationImage.set(true);
    this.imageUrls.resolveUnsplashPhotoPage(url).subscribe({
      next: ({ imageUrl }) => {
        this.destinationDraft = { ...this.destinationDraft, imageUrl };
        this.resolvingDestinationImage.set(false);
        afterResolve?.();
      },
      error: () => {
        this.resolvingDestinationImage.set(false);
        this.destinationError.set('Could not open that Unsplash photo page. Use a direct public HTTP or HTTPS image URL.');
      },
    });
  }

  removeDestination(place: Destination): void {
    this.destinationError.set('');
    this.content.removeDestination(place.id, place.kind).subscribe({
      next: () => this.destinationMessage.set(`"${place.name}" was removed.`),
      error: (error: unknown) => this.destinationError.set(writeError(error, 'Could not remove this place')),
    });
  }

  addHotel(): void {
    this.hotelError.set('');
    this.hotelMessage.set('');
    if (!isImageSource(this.hotelDraft.imageUrl)) {
      this.hotelError.set('Enter a public image URL or choose a supported image file.');
      return;
    }
    const draft = { ...this.hotelDraft, name: this.hotelDraft.name.trim(), location: this.hotelDraft.location.trim(), description: this.hotelDraft.description.trim(), imageUrl: this.hotelDraft.imageUrl.trim(), amenities: this.hotelDraft.amenities.trim() };
    this.hotels.addHotel(draft).subscribe({
      next: () => {
        this.hotelDraft = { name: '', location: '', description: '', imageUrl: '', pricePerNight: 0, amenities: '' };
        this.hotelMessage.set('Accommodation added to the public stay page.');
      },
      error: (error: unknown) => this.hotelError.set(writeError(error, 'Could not add this accommodation')),
    });
  }

  removeHotel(id: string): void {
    this.hotelError.set('');
    this.hotels.removeHotel(id).subscribe({
      next: () => this.hotelMessage.set('Accommodation removed.'),
      error: (error: unknown) => this.hotelError.set(writeError(error, 'Could not remove this accommodation')),
    });
  }

  addEvent(): void {
    this.eventError.set('');
    this.eventMessage.set('');
    if (!isImageSource(this.eventDraft.imageUrl)) {
      this.eventError.set('Enter a public image URL or choose a supported image file.');
      return;
    }
    const draft = { ...this.eventDraft, title: this.eventDraft.title.trim(), date: this.eventDraft.date.trim(), location: this.eventDraft.location.trim(), description: this.eventDraft.description.trim(), imageUrl: this.eventDraft.imageUrl.trim() };
    this.content.addEvent(draft).subscribe({
      next: () => {
        this.eventDraft = { title: '', date: '', location: '', description: '', imageUrl: '' };
        this.eventMessage.set('Event published on the public events page.');
      },
      error: (error: unknown) => this.eventError.set(writeError(error, 'Could not add this event')),
    });
  }

  removeEvent(id: string): void {
    this.eventError.set('');
    this.content.removeEvent(id).subscribe({
      next: () => this.eventMessage.set('Event removed.'),
      error: (error: unknown) => this.eventError.set(writeError(error, 'Could not remove this event')),
    });
  }

  addImage(): void {
    this.imageError.set('');
    this.imageMessage.set('');
    if (!isImageSource(this.imageDraft.src)) {
      this.imageError.set('Enter a public image URL or choose a supported image file.');
      return;
    }
    const draft = { ...this.imageDraft, src: this.imageDraft.src.trim(), alt: this.imageDraft.alt.trim(), category: this.imageDraft.category.trim() };
    this.gallery.addImage(draft).subscribe({
      next: () => {
        this.imageDraft = { src: '', alt: '', category: '' };
        this.imageMessage.set('Image added to the public gallery.');
      },
      error: (error: unknown) => this.imageError.set(writeError(error, 'Could not add this image')),
    });
  }

  addOffer(): void {
    this.offerError.set('');
    this.offerMessage.set('');
    const draft = { ...this.offerDraft, title: this.offerDraft.title.trim(), description: this.offerDraft.description.trim(), code: this.offerDraft.code.trim().toUpperCase() };
    this.offers.addOffer(draft).subscribe({
      next: () => {
        this.offerDraft = emptyOffer();
        this.offerMessage.set('Offer published successfully.');
      },
      error: (error: unknown) => this.offerError.set(writeError(error, 'Could not publish this offer')),
    });
  }

  removeImage(image: GalleryImage): void {
    this.imageError.set('');
    this.gallery.removeImage(image.id).subscribe({
      next: () => this.imageMessage.set('Image removed from the gallery.'),
      error: (error: unknown) => this.imageError.set(writeError(error, 'Could not remove this image')),
    });
  }

  removeOffer(id: string): void {
    this.offerError.set('');
    this.offers.removeOffer(id).subscribe({
      next: () => this.offerMessage.set('Offer removed.'),
      error: (error: unknown) => this.offerError.set(writeError(error, 'Could not remove this offer')),
    });
  }

  addContactDetails(): void {
    this.contactError.set('');
    this.contactMessage.set('');
    const draft = {
      phone: this.contactDraft.phone.trim(),
      email: this.contactDraft.email.trim(),
      officeHours: this.contactDraft.officeHours.trim(),
    };
    this.contacts.add(draft).subscribe({
      next: () => {
        this.contactDraft = { phone: '', email: '', officeHours: '' };
        this.contactMessage.set('Contact details saved to the public contact page.');
      },
      error: (error: unknown) => this.contactError.set(writeError(error, 'Could not save contact details')),
    });
  }

  removeContactDetails(id: string): void {
    this.contactError.set('');
    this.contacts.remove(id).subscribe({
      next: () => this.contactMessage.set('Contact details removed.'),
      error: (error: unknown) => this.contactError.set(writeError(error, 'Could not remove contact details')),
    });
  }

  tripTitle(id: string): string {
    return this.trips.trips().find((trip) => trip.id === id)?.title ?? 'Journey no longer listed';
  }
}

function isUnsplashPhotoPage(value: string): boolean {
  try {
    const url = new URL(value);
    return (url.hostname === 'unsplash.com' || url.hostname === 'www.unsplash.com')
      && url.pathname.startsWith('/photos/');
  } catch {
    return false;
  }
}

function writeError(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) return `${fallback} (request failed: ${error.status}).`;
  return error instanceof Error ? error.message : `${fallback}.`;
}
