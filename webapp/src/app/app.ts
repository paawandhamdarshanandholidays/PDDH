import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ContactDetailsService } from './core/services/contact-details.service';
import { ContentService } from './core/services/content.service';
import { GalleryService } from './core/services/gallery.service';
import { HotelService } from './core/services/hotel.service';
import { OfferService } from './core/services/offer.service';
import { TripService } from './core/services/trip.service';
import { Footer } from './shared/components/footer/footer';
import { Navbar } from './shared/components/navbar/navbar';

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    Footer,
    Navbar
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  constructor() {
    inject(ContentService);
    inject(GalleryService);
    inject(HotelService);
    inject(OfferService);
    inject(TripService);
    inject(ContactDetailsService);
  }
}
