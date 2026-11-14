import { Component, inject } from '@angular/core';
import { ContentService } from '../../core/services/content.service';
import { TempleCard } from '../../shared/components/cards/temple-card';

@Component({
  selector: 'app-temples',
  standalone: true,
  imports: [TempleCard],
  template: `
    <section class="page-hero page-hero--temples"><p class="eyebrow eyebrow--light">Places of devotion</p><h1>Sacred temples</h1><p>Discover the stories, rituals and living traditions of India's most cherished places.</p></section>
    <section class="section"><div class="section-heading"><div><p class="eyebrow">Explore at your own pace</p><h2>Places that call you closer</h2></div><span class="result-count">{{ content.temples().length }} temples</span></div>
      <div class="card-grid">@for (temple of content.temples(); track temple.id) { <app-temple-card [place]="temple" /> }</div>
    </section>
  `,
})
export class Temples {
  readonly content = inject(ContentService);
}
