import { Component, inject } from '@angular/core';
import { OfferService } from '../../core/services/offer.service';
import { TripService } from '../../core/services/trip.service';
import { OfferCard } from '../../shared/components/cards/offer-card';

@Component({
  selector: 'app-offers',
  standalone: true,
  imports: [OfferCard],
  template: `
    <section class="page-hero page-hero--offers"><p class="eyebrow eyebrow--light">A little more room to explore</p><h1>Journey offers</h1><p>Thoughtful savings on selected journeys. Find an offer that feels right and start planning.</p></section>
    <section class="section"><div class="section-heading"><div><p class="eyebrow">Made for your next journey</p><h2>Current offers</h2></div><span class="result-count">{{ offers.activeOffers().length }} available</span></div>
      <div class="offer-grid">@for (offer of offers.activeOffers(); track offer.id) { <app-offer-card [offer]="offer" [trip]="findTrip(offer.tripId)" /> } @empty { <div class="empty-state"><h3>No active offers right now</h3><p>Check back soon for a new reason to explore.</p></div> }</div>
    </section>
  `,
})
export class Offers {
  readonly offers = inject(OfferService);
  private readonly trips = inject(TripService);

  findTrip(id: string) {
    return this.trips.trips().find((trip) => trip.id === id);
  }
}
