import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Destination } from '../../../core/models/content.model';

@Component({
  selector: 'app-temple-card',
  standalone: true,
  imports: [RouterLink],
  template: `
    <article class="content-card">
      <img class="content-card__image" [src]="place().imageUrl" [alt]="place().name" loading="lazy">
      <div class="content-card__body">
        <span class="eyebrow">{{ place().category }} · {{ place().region }}</span>
        <h3>{{ place().name }}</h3>
        <p>{{ place().description }}</p>
        <a class="text-link" routerLink="/booking">Plan a visit <span aria-hidden="true">→</span></a>
      </div>
    </article>
  `,
})
export class TempleCard {
  readonly place = input.required<Destination>();
}
