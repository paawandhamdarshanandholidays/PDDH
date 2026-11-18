import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ContentService } from '../../core/services/content.service';
import { GalleryService } from '../../core/services/gallery.service';
import { HotelService } from '../../core/services/hotel.service';
import { OfferService } from '../../core/services/offer.service';
import { TripService } from '../../core/services/trip.service';

@Component({
  selector: 'app-ceo-dashboard',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="admin-page">
      <div class="admin-heading"><div><p class="eyebrow">Executive workspace</p><h1>CEO dashboard</h1><p>Full control of journeys, destinations, stays, events, offers and images.</p></div><span class="admin-count">All management areas</span></div>
      <div class="ceo-stats">
        @for (stat of stats(); track stat.label) { <article><span>{{ stat.label }}</span><strong>{{ stat.value }}</strong></article> }
      </div>
      <div class="ceo-actions">
        <a class="ceo-action" routerLink="/admin/trips"><span class="ceo-action__icon">↗</span><div><h2>Trips & journeys</h2><p>Create, edit, feature or remove trip listings.</p></div><span class="text-link">Manage →</span></a>
        <a class="ceo-action" routerLink="/admin/content"><span class="ceo-action__icon">✳</span><div><h2>Website content</h2><p>Manage temples, places, hotels, events, offers and gallery images.</p></div><span class="text-link">Manage →</span></a>
        <a class="ceo-action" routerLink="/portal/users"><span class="ceo-action__icon">♙</span><div><h2>People & profiles</h2><p>Create and manage administrator, manager, staff and customer profiles.</p></div><span class="text-link">Manage →</span></a>
        <a class="ceo-action" routerLink="/portal/user-directory"><span class="ceo-action__icon">♧</span><div><h2>User directory</h2><p>Review customer contact details and add manual records.</p></div><span class="text-link">Open →</span></a>
        <a class="ceo-action" routerLink="/portal/bookings"><span class="ceo-action__icon">▤</span><div><h2>Customer bookings</h2><p>Review reservation requests and update their status.</p></div><span class="text-link">Review →</span></a>
        <a class="ceo-action" routerLink="/portal/management"><span class="ceo-action__icon">✉</span><div><h2>Bookings, enquiries & contact</h2><p>Search customer requests and manage enquiry status.</p></div><span class="text-link">Manage →</span></a>
      </div>
      <p class="admin-security-note">The dashboard uses role-aware frontend routes. Your API must also enforce CEO authorization for all protected data and booking operations.</p>
    </section>
  `,
})
export class CeoDashboard {
  private readonly trips = inject(TripService);
  private readonly content = inject(ContentService);
  private readonly hotels = inject(HotelService);
  private readonly gallery = inject(GalleryService);
  private readonly offers = inject(OfferService);
  readonly stats = computed(() => [
    { label: 'Trips', value: this.trips.trips().length },
    { label: 'Temples', value: this.content.temples().length },
    { label: 'Places', value: this.content.places().length },
    { label: 'Hotels & stays', value: this.hotels.hotels().length },
    { label: 'Gallery items', value: this.gallery.images().length },
    { label: 'Offers', value: this.offers.offers().length },
    { label: 'Events', value: this.content.events().length },
  ]);
}
