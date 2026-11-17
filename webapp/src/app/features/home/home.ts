import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ContentService } from '../../core/services/content.service';
import { TripService } from '../../core/services/trip.service';
import { OfferService } from '../../core/services/offer.service';
import { EventCard } from '../../shared/components/cards/event-card';
import { TempleCard } from '../../shared/components/cards/temple-card';
import { TripCard } from '../../shared/components/cards/trip-card';
import { Offer } from '../../core/models/offer.model';
import { OfferCard } from '../../shared/components/cards/offer-card';

@Component({
  selector: 'app-home',
  imports: [RouterLink, TripCard, TempleCard, EventCard, OfferCard],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home {
  private readonly tripService = inject(TripService);
  private readonly content = inject(ContentService);
  private readonly offerService = inject(OfferService);
  readonly trips = this.tripService.featuredTrips;
  readonly offers = this.offerService.activeOffers;
  readonly temples = computed(() => this.content.temples().slice(0, 3));
  readonly events = computed(() => this.content.events().slice(0, 2));

  findTrip(id: Offer['tripId']) {
    return this.tripService.trips().find((trip) => trip.id === id);
  }
}
