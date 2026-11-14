import { Component, input } from '@angular/core';
import { Event } from '../../../core/models/content.model';

@Component({
  selector: 'app-event-card',
  standalone: true,
  template: `
    <article class="content-card event-card">
      <img class="content-card__image" [src]="event().imageUrl" [alt]="event().title" loading="lazy">
      <div class="content-card__body">
        <span class="eyebrow">{{ event().date }} · {{ event().location }}</span>
        <h3>{{ event().title }}</h3>
        <p>{{ event().description }}</p>
      </div>
    </article>
  `,
})
export class EventCard {
  readonly event = input.required<Event>();
}
