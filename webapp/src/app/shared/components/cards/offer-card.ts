import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Offer } from '../../../core/models/offer.model';
import { Trip } from '../../../core/models/trip.model';

@Component({
  selector: 'app-offer-card',
  standalone: true,
  imports: [DatePipe, RouterLink],
  template: `
    <article class="offer-card">
      @if (offer().imageUrl) { <img class="offer-card__image" [src]="offer().imageUrl" [alt]="offer().title" loading="lazy"> }
      <span class="offer-card__discount">{{ offer().discountPercent }}% <small>OFF</small></span>
      <p class="eyebrow">A little something for your journey</p>
      <h3>{{ offer().title }}</h3>
      <p class="offer-card__description">{{ offer().description }}</p>
      @if (trip()) { <p class="offer-card__trip">For {{ trip()!.title }}</p> }
      <div class="offer-card__code"><span>Use code</span><strong>{{ offer().code }}</strong></div>
      <p class="offer-card__expiry">Valid until {{ offer().validUntil | date:'mediumDate' }}</p>
      <a class="text-link" [routerLink]="['/booking']" [queryParams]="{ trip: offer().tripId }">Explore offer <span aria-hidden="true">→</span></a>
    </article>
  `,
})
export class OfferCard {
  readonly offer = input.required<Offer>();
  readonly trip = input<Trip | undefined>();
}
