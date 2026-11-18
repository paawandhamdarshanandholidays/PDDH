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
import { TripService } from './trip.service';

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
  let trips: TripService;

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
    trips = TestBed.inject(TripService);
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
    const uploadedImage = 'data:image/png;base64,iVBORw0KGgo=';
    const destination = {
      kind: 'Temple' as const,
      name: 'New temple',
      region: 'Ayodhya',
      description: 'A new listing',
      imageUrl: uploadedImage,
      category: 'Temple' as const,
    };
    const event = {
      title: 'Festival',
      date: '2026-12-01',
      location: 'Ayodhya',
      description: 'Festival event',
      imageUrl: uploadedImage,
    };
    const image = { src: uploadedImage, alt: 'Temple', category: 'Ayodhya' };
    const hotel = {
      name: 'Dharamshala',
      location: 'Ayodhya',
      description: 'Guest rooms',
      imageUrl: uploadedImage,
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
      imageUrl: uploadedImage,
    };
    const trip = {
      title: 'Ayodhya journey',
      destination: 'Ayodhya',
      summary: 'A new journey',
      imageUrl: uploadedImage,
      startDate: '2026-12-01',
      durationDays: 2,
      price: 2500,
      category: 'Pilgrimage' as const,
      featured: false,
    };
    const requests: Array<{ url: string; body: object; response: object }> = [
      { url: '/api/catalog/temples', body: destination, response: { ...destination, imageUrl: '/api/catalog/temples/a1/image', id: 'a1' } },
      { url: '/api/catalog/places', body: { ...destination, kind: 'Place', category: 'Heritage' }, response: { ...destination, kind: 'Place', category: 'Heritage', imageUrl: '/api/catalog/places/b2/image', id: 'b2' } },
      { url: '/api/catalog/events', body: event, response: { ...event, imageUrl: '/api/catalog/events/e1/image', id: 'e1' } },
      { url: '/api/catalog/gallery', body: image, response: { ...image, src: '/api/catalog/gallery/ca11/image', id: 'ca11' } },
      { url: '/api/catalog/hotels', body: hotel, response: { ...hotel, imageUrl: '/api/catalog/hotels/ab01/image', id: 'ab01' } },
      { url: '/api/catalog/offers', body: offer, response: { ...offer, imageUrl: '/api/catalog/offers/0ffe/image', id: '0ffe' } },
      { url: '/api/catalog/trips', body: trip, response: { ...trip, imageUrl: '/api/catalog/trips/a11ce/image', id: 'a11ce' } },
    ];

    content.addDestination(destination).subscribe();
    expectWrite(requests[0]);
    content.addDestination({ ...destination, kind: 'Place', category: 'Heritage' }).subscribe();
    expectWrite(requests[1]);
    content.addEvent(event).subscribe();
    expectWrite(requests[2]);
    gallery.addMedia(image).subscribe();
    expectWrite(requests[3]);
    hotels.addHotel(hotel).subscribe();
    expectWrite(requests[4]);
    offers.addOffer(offer).subscribe();
    expectWrite(requests[5]);
    trips.addTrip(trip).subscribe();
    expectWrite(requests[6]);

    expect(content.temples()[0].id).toBe('a1');
    expect(content.places()[0].id).toBe('b2');
    expect(content.events()[0].id).toBe('e1');
    expect(gallery.images()[0].id).toBe('ca11');
    expect(hotels.hotels()[0].id).toBe('ab01');
    expect(offers.offers()[0].id).toBe('0ffe');
    expect(trips.trips()[0].id).toBe('a11ce');
    expect(content.temples()[0].imageUrl).toBe('http://localhost:9091/api/catalog/temples/a1/image');
    expect(gallery.images()[0].src).toBe('http://localhost:9091/api/catalog/gallery/ca11/image');

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
    gallery.addMedia(image).subscribe();
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
    gallery.addMedia(draft).subscribe({ error: () => failed = true });
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
  });

  it('persists a public video URL with its media type', () => {
    const video = {
      src: 'https://youtu.be/aqz-KE-bpKQ',
      alt: 'Pilgrimage video',
      category: 'Ayodhya',
      mediaType: 'VIDEO' as const,
    };
    gallery.addMedia(video).subscribe();
    const request = http.expectOne('http://localhost:9091/api/catalog/gallery');
    expect(request.request.body).toEqual(video);
    request.flush({ ...video, id: 'video-1' });

    expect(gallery.images()[0]).toEqual(expect.objectContaining({
      id: 'video-1',
      mediaType: 'VIDEO',
      src: video.src,
    }));

    auth.logout();

    expect(gallery.images()).toEqual([]);
    expect(localStorage.getItem('pddh.gallery.v1')).toBeNull();
    expect(content.temples()).toEqual([]);
    expect(hotels.hotels()).toEqual([]);
    expect(offers.offers()).toEqual([]);
    expect(contacts.items()).toEqual([]);
  });

  it('rejects malformed inline image data without sending or caching it', () => {
    let writeSucceeded = false;
    let writeFailed = false;
    gallery.addMedia({ src: 'data:image/jpeg;base64,abc', alt: 'Photo', category: 'Ayodhya' })
      .subscribe({ next: () => writeSucceeded = true, error: () => writeFailed = true });

    http.expectNone('http://localhost:9091/api/catalog/gallery');
    expect(writeFailed).toBe(true);
    expect(writeSucceeded).toBe(false);
    expect(gallery.images().some((image) => image.src.startsWith('data:'))).toBe(false);
  });

  function flushInitialLists(): void {
    for (const resource of ['temples', 'places', 'events', 'gallery', 'hotels', 'offers', 'contact', 'trips']) {
      http.expectOne(`http://localhost:9091/api/catalog/${resource}`).flush([]);
    }
  }
});
