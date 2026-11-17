import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TripService } from '../../core/services/trip.service';
import { ContentService } from '../../core/services/content.service';
import { TripCard } from '../../shared/components/cards/trip-card';
import { TempleCard } from '../../shared/components/cards/temple-card';

@Component({
  selector: 'app-places',
  standalone: true,
  imports: [RouterLink, TripCard, TempleCard],
  template: `
    <section class="page-hero page-hero--places"><p class="eyebrow eyebrow--light">Handpicked journeys</p><h1>Find your place</h1><p>From ancient river cities to the quiet corners of Braj, let the road unfold.</p></section>
    <section class="section section--tint"><div class="section-heading"><div><p class="eyebrow">Discover along the way</p><h2>Places worth a pause</h2></div><span class="result-count">{{ content.places().length }} places</span></div>
      <div class="card-grid">@for (place of content.places(); track place.id) { <app-temple-card [place]="place" /> }</div>
      @if (!content.places().length) { <p class="empty-note">Our next destination is being prepared. Check back soon.</p> }
    </section>
    <section class="section">
      <div class="section-heading"><div><p class="eyebrow">Go somewhere meaningful</p><h2>Journeys made for you</h2></div></div>
      <div class="filter-row" aria-label="Filter trips">
        @for (category of categories; track category) { <button type="button" class="filter-chip" [class.filter-chip--active]="selectedCategory() === category" (click)="selectedCategory.set(category)">{{ category }}</button> }
      </div>
      <div class="card-grid card-grid--trips">
        @for (trip of filteredTrips(); track trip.id) { <app-trip-card [trip]="trip" /> }
      </div>
      @if (!filteredTrips().length) { <div class="empty-state"><h3>No journeys in this category yet</h3><p>Choose another category or contact us to plan a custom itinerary.</p><a class="button" routerLink="/contact">Plan a custom journey</a></div> }
    </section>
  `,
})
export class Places {
  private readonly tripService = inject(TripService);
  readonly content = inject(ContentService);
  readonly categories = ['All', 'Pilgrimage', 'Heritage', 'Nature'] as const;
  readonly selectedCategory = signal<string>('All');
  readonly filteredTrips = computed(() => {
    const category = this.selectedCategory();
    return this.tripService.trips().filter((trip) => category === 'All' || trip.category === category);
  });
}
