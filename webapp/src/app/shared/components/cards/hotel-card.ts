import { CurrencyPipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { Hotel } from '../../../core/models/hotel.model';

@Component({
  selector: 'app-hotel-card',
  standalone: true,
  imports: [CurrencyPipe],
  template: `
    <article class="content-card hotel-card">
      <img class="content-card__image" [src]="hotel().imageUrl" [alt]="hotel().name" loading="lazy">
      <div class="content-card__body">
        <p class="eyebrow">{{ hotel().location }}</p>
        <h3>{{ hotel().name }}</h3>
        <p>{{ hotel().description }}</p>
        <div class="hotel-card__amenities">{{ hotel().amenities }}</div>
        <strong class="hotel-card__price">{{ hotel().pricePerNight | currency:'INR':'symbol':'1.0-0' }} <small>/ night</small></strong>
      </div>
    </article>
  `,
})
export class HotelCard {
  readonly hotel = input.required<Hotel>();
}
