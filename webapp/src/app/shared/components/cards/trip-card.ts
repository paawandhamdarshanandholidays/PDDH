import { CurrencyPipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Trip } from '../../../core/models/trip.model';

@Component({
  selector: 'app-trip-card',
  standalone: true,
  imports: [CurrencyPipe, RouterLink],
  template: `
    <article class="trip-card">
      <div class="trip-card__photo">
        <img [src]="trip().imageUrl" [alt]="trip().destination" loading="lazy">
        <span class="trip-card__tag">{{ trip().category }}</span>
      </div>
      <div class="trip-card__body">
        <p class="eyebrow">{{ trip().destination }}</p>
        <h3>{{ trip().title }}</h3>
        <p>{{ trip().summary }}</p>
        <div class="trip-card__meta"><span>{{ trip().durationDays }} days</span><span>From {{ trip().price | currency:'INR':'symbol':'1.0-0' }}</span></div>
        <a class="text-link" [routerLink]="['/booking']" [queryParams]="{ trip: trip().id }">Explore this trip <span aria-hidden="true">→</span></a>
      </div>
    </article>
  `,
})
export class TripCard {
  readonly trip = input.required<Trip>();
}
