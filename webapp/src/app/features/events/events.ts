import { Component, inject } from '@angular/core';
import { ContentService } from '../../core/services/content.service';
import { EventCard } from '../../shared/components/cards/event-card';

@Component({
  selector: 'app-events',
  standalone: true,
  imports: [EventCard],
  template: `
    <section class="page-hero page-hero--events"><p class="eyebrow eyebrow--light">Gather in good company</p><h1>Events & experiences</h1><p>Make space for the rituals, celebrations and small moments that make a journey yours.</p></section>
    <section class="section"><div class="section-heading"><div><p class="eyebrow">What’s happening</p><h2>Meet us along the way</h2></div></div>
      <div class="card-grid">@for (event of content.events(); track event.id) { <app-event-card [event]="event" /> }</div>
    </section>
  `,
})
export class Events {
  readonly content = inject(ContentService);
}
