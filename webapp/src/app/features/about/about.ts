import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-about',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="page-hero page-hero--about"><p class="eyebrow eyebrow--light">The heart behind every journey</p><h1>Travel with meaning</h1><p>We believe the best journeys are measured not only in miles, but in what we carry home.</p></section>
    <section class="section about-story"><div class="about-image"><img src="https://images.unsplash.com/photo-1548013146-72479768bada?auto=format&fit=crop&w=1200&q=85" alt="Temple architecture in the warm evening light"></div><div><p class="eyebrow">Our story</p><h2>A little more care changes everything.</h2><p>Pawan Dham Darshan & Holidays was created for travellers who want to experience India's sacred places with respect, comfort and genuine curiosity.</p><p>We bring together thoughtful itineraries, local knowledge and personal support, so you can be present for the moments that matter — whether it is your first darshan or a return to a place you love.</p><a class="button" routerLink="/contact">Meet your travel team <span aria-hidden="true">→</span></a></div></section>
    <section class="section section--tint"><div class="section-heading"><div><p class="eyebrow">What guides us</p><h2>Our promises to every traveller</h2></div></div><div class="values-grid"><article><span>01</span><h3>Care, always</h3><p>Clear communication and personal assistance at each step of your journey.</p></article><article><span>02</span><h3>Travel respectfully</h3><p>We honour the people, customs and places that welcome us along the way.</p></article><article><span>03</span><h3>Make it yours</h3><p>Every itinerary should leave room for your pace, interests and moments of discovery.</p></article></div></section>
    <section class="callout"><div><p class="eyebrow eyebrow--light">A meaningful journey is closer than you think</p><h2>Where would you like to begin?</h2></div><a class="button button--light" routerLink="/places">Explore journeys <span aria-hidden="true">→</span></a></section>
  `,
})
export class About {}
