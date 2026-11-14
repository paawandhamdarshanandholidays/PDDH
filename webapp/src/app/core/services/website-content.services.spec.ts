import { Component } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { authInterceptor } from '../interceptors/auth.interceptor';
import { AuthService } from './auth.service';
import { ContentService } from './content.service';
import { GalleryService } from './gallery.service';
import { HotelService } from './hotel.service';
import { OfferService } from './offer.service';
import { ContactDetailsService } from './contact-details.service';

@Component({ standalone: true, template: '' })
class ContentServicesHost {}

describe('website content API persistence', () => {
  let http: HttpTestingController;
  let auth: AuthService;
  let content: ContentService;
  let gallery: GalleryService;
  let hotels: HotelService;
  let offers: OfferService;
  let contacts: ContactDetailsService;

  beforeEach(() => {
    sessionStorage.setItem('pddh.accessToken', 'test-token');
    sessionStorage.setItem('pddh.accountRole', 'ADMIN');
    TestBed.configureTestingModule({
      imports: [ContentServicesHost],
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    content = TestBed.inject(ContentService);
    gallery = TestBed.inject(GalleryService);
    hotels = TestBed.inject(HotelService);
    offers = TestBed.inject(OfferService);
    contacts = TestBed.inject(ContactDetailsService);
    const fixture = TestBed.createComponent(ContentServicesHost);
    fixture.detectChanges();
    flushInitialLists();
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
    sessionStorage.clear();
    localStorage.clear();
  });

  it('creates every content type through its database API with authenticated idempotent writes', () => {
    const destination = {
      kind: 'Temple' as const,
      name: 'New temple',
      region: 'Ayodhya',
      description: 'A new listing',
      imageUrl: 'https://example.test/temple.jpg',
      category: 'Temple' as const,
    };
    const event = {
      title: 'Festival',
      date: '2026-12-01',
      location: 'Ayodhya',
      description: 'Festival event',
      imageUrl: 'https://example.test/event.jpg',
    };
    const image = { src: 'https://example.test/gallery.jpg', alt: 'Temple', category: 'Ayodhya' };
    const hotel = {
      name: 'Dharamshala',
      location: 'Ayodhya',
      description: 'Guest rooms',
      imageUrl: 'https://example.test/hotel.jpg',
      pricePerNight: 800,
      amenities: 'Family rooms',
    };
    const offer = {
      title: 'Festival offer',
      description: 'Save on a trip',
      code: 'FEST10',
      discountPercent: 10,
      validUntil: '2026-12-31',
      tripId: 'trip-1',
      active: true,
    };
    const requests: Array<{ url: string; body: object; response: object }> = [
      { url: '/api/catalog/temples', body: destination, response: { ...destination, id: 'temple-1' } },
      { url: '/api/catalog/places', body: { ...destination, kind: 'Place', category: 'Heritage' }, response: { ...destination, kind: 'Place', category: 'Heritage', id: 'place-1' } },
      { url: '/api/catalog/events', body: event, response: { ...event, id: 'event-1' } },
      { url: '/api/catalog/gallery', body: image, response: { ...image, id: 'image-1' } },
      { url: '/api/catalog/hotels', body: hotel, response: { ...hotel, id: 'hotel-1' } },
      { url: '/api/catalog/offers', body: offer, response: { ...offer, id: 'offer-1' } },
    ];

    content.addDestination(destination).subscribe();
    expectWrite(requests[0]);
    content.addDestination({ ...destination, kind: 'Place', category: 'Heritage' }).subscribe();
    expectWrite(requests[1]);
    content.addEvent(event).subscribe();
    expectWrite(requests[2]);
    gallery.addImage(image).subscribe();
    expectWrite(requests[3]);
    hotels.addHotel(hotel).subscribe();
    expectWrite(requests[4]);
    offers.addOffer(offer).subscribe();
    expectWrite(requests[5]);

    expect(content.temples()[0].id).toBe('temple-1');
    expect(content.places()[0].id).toBe('place-1');
    expect(content.events()[0].id).toBe('event-1');
    expect(gallery.images()[0].id).toBe('image-1');
    expect(hotels.hotels()[0].id).toBe('hotel-1');
    expect(offers.offers()[0].id).toBe('offer-1');

    function expectWrite(request: { url: string; body: object; response: object }): void {
      const requestUrl = `http://localhost:9091${request.url}`;
      const pending = http.expectOne(requestUrl);
      expect(pending.request.method).toBe('POST');
      expect(pending.request.body).toEqual(request.body);
      expect(pending.request.headers.get('Authorization')).toBe('Bearer test-token');
      expect(pending.request.headers.get('Idempotency-Key')).toMatch(/^[\x21-\x7E]{8,128}$/);
      pending.flush(request.response);
    }
  });

  it('deletes records through their APIs and changes the in-memory lists only on success', () => {
    const image = { src: 'https://example.test/photo.jpg', alt: 'Photo', category: 'Ayodhya' };
    gallery.addImage(image).subscribe();
    http.expectOne('http://localhost:9091/api/catalog/gallery').flush({ ...image, id: 'photo-1' });
    expect(gallery.images()).toHaveLength(1);

    let deleted = false;
    gallery.removeImage('photo-1').subscribe(() => deleted = true);
    const request = http.expectOne('http://localhost:9091/api/catalog/gallery/photo-1');
    expect(request.request.method).toBe('DELETE');
    request.flush(null);

    expect(deleted).toBe(true);
    expect(gallery.images()).toHaveLength(0);
  });

  it('does not add a record after an API authorization failure', () => {
    const draft = { src: 'https://example.test/photo.jpg', alt: 'Photo', category: 'Ayodhya' };
    let failed = false;
    gallery.addImage(draft).subscribe({ error: () => failed = true });
    http.expectOne('http://localhost:9091/api/catalog/gallery').flush({}, {
      status: 403,
      statusText: 'Forbidden',
    });

    expect(failed).toBe(true);
    expect(gallery.images()).toHaveLength(0);
  });

  it('creates, updates, and deletes contact details through the database API', () => {
    const draft = { phone: '+91 1234567890', email: 'hello@example.test', officeHours: 'Weekdays' };
    contacts.add(draft).subscribe();
    const create = http.expectOne('http://localhost:9091/api/catalog/contact');
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(draft);
    expect(create.request.headers.get('Authorization')).toBe('Bearer test-token');
    expect(create.request.headers.get('Idempotency-Key')).toMatch(/^[\x21-\x7E]{8,128}$/);
    create.flush({ ...draft, id: 'contact-1' });

    contacts.update('contact-1', { ...draft, phone: '+91 0987654321' }).subscribe();
    const update = http.expectOne('http://localhost:9091/api/catalog/contact/contact-1');
    expect(update.request.method).toBe('PUT');
    update.flush({ ...draft, phone: '+91 0987654321', id: 'contact-1' });

    contacts.remove('contact-1').subscribe();
    const remove = http.expectOne('http://localhost:9091/api/catalog/contact/contact-1');
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
    expect(contacts.items()).toEqual([]);
  });

  it('clears loaded content cache and legacy browser data on logout', () => {
    localStorage.setItem('pddh.gallery.v1', 'legacy');
    expect(gallery.images().length).toBe(0);

    auth.logout();

    expect(gallery.images()).toEqual([]);
    expect(localStorage.getItem('pddh.gallery.v1')).toBeNull();
    expect(content.temples()).toEqual([]);
    expect(hotels.hotels()).toEqual([]);
    expect(offers.offers()).toEqual([]);
    expect(contacts.items()).toEqual([]);
  });

  it('rejects inline image data without sending or caching it', () => {
    let writeSucceeded = false;
    let writeFailed = false;
    gallery.addImage({ src: 'data:image/jpeg;base64,abc', alt: 'Photo', category: 'Ayodhya' })
      .subscribe({ next: () => writeSucceeded = true, error: () => writeFailed = true });

    http.expectNone('http://localhost:9091/api/catalog/gallery');
    expect(writeFailed).toBe(true);
    expect(writeSucceeded).toBe(false);
    expect(gallery.images().some((image) => image.src.startsWith('data:'))).toBe(false);
  });

  function flushInitialLists(): void {
    for (const resource of ['temples', 'places', 'events', 'gallery', 'hotels', 'offers', 'contact']) {
      http.expectOne(`http://localhost:9091/api/catalog/${resource}`).flush([]);
    }
  }
});
