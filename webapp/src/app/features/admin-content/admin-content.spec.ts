import { provideHttpClient } from '@angular/common/http';
import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { of, Subject, throwError } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { ContentService } from '../../core/services/content.service';
import { GalleryService } from '../../core/services/gallery.service';
import { HotelService } from '../../core/services/hotel.service';
import { ImageUrlService } from '../../core/services/image-url.service';
import { OfferService } from '../../core/services/offer.service';
import { TripService } from '../../core/services/trip.service';
import { ContactDetailsService } from '../../core/services/contact-details.service';
import { AdminContent } from './admin-content';

describe('AdminContent', () => {
  let fixture: ComponentFixture<AdminContent>;
  let component: AdminContent;
  let logoutEvents: Subject<void>;
  let content: {
    temples: ReturnType<typeof signal>;
    places: ReturnType<typeof signal>;
    events: ReturnType<typeof signal>;
    addDestination: ReturnType<typeof vi.fn>;
    removeDestination: ReturnType<typeof vi.fn>;
    addEvent: ReturnType<typeof vi.fn>;
    removeEvent: ReturnType<typeof vi.fn>;
  };
  let gallery: {
    images: ReturnType<typeof signal>;
    addMedia: ReturnType<typeof vi.fn>;
    removeImage: ReturnType<typeof vi.fn>;
  };
  let hotels: {
    hotels: ReturnType<typeof signal>;
    addHotel: ReturnType<typeof vi.fn>;
    removeHotel: ReturnType<typeof vi.fn>;
  };
  let offers: {
    offers: ReturnType<typeof signal>;
    addOffer: ReturnType<typeof vi.fn>;
    removeOffer: ReturnType<typeof vi.fn>;
  };
  let contacts: {
    items: ReturnType<typeof signal>;
    add: ReturnType<typeof vi.fn>;
    remove: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    logoutEvents = new Subject<void>();
    content = {
      temples: signal([]),
      places: signal([]),
      events: signal([]),
      addDestination: vi.fn(() => of({ id: 'destination-1' })),
      removeDestination: vi.fn(() => of(void 0)),
      addEvent: vi.fn(() => of({ id: 'event-1' })),
      removeEvent: vi.fn(() => of(void 0)),
    };
    gallery = {
      images: signal([]),
      addMedia: vi.fn(() => of({ id: 'image-1' })),
      removeImage: vi.fn(() => of(void 0)),
    };
    hotels = {
      hotels: signal([]),
      addHotel: vi.fn(() => of({ id: 'hotel-1' })),
      removeHotel: vi.fn(() => of(void 0)),
    };
    offers = {
      offers: signal([]),
      addOffer: vi.fn(() => of({ id: 'offer-1' })),
      removeOffer: vi.fn(() => of(void 0)),
    };
    contacts = {
      items: signal([]),
      add: vi.fn(() => of({ id: 'contact-1' })),
      remove: vi.fn(() => of(void 0)),
    };
    await TestBed.configureTestingModule({
      imports: [AdminContent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        { provide: AuthService, useValue: { logoutEvents: logoutEvents.asObservable() } },
        { provide: ContentService, useValue: content },
        { provide: GalleryService, useValue: gallery },
        { provide: HotelService, useValue: hotels },
        { provide: OfferService, useValue: offers },
        { provide: ContactDetailsService, useValue: contacts },
        { provide: TripService, useValue: { trips: signal([]) } },
        { provide: ImageUrlService, useValue: { resolveUnsplashPhotoPage: vi.fn(() => of({ imageUrl: '' })) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(AdminContent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('submits temple, place, accommodation, event, gallery and offer data to their services', () => {
    component.destinationDraft = {
      kind: 'Temple',
      name: ' New temple ',
      region: 'Ayodhya',
      description: 'A new listing',
      imageUrl: 'https://example.test/temple.jpg',
      category: 'Temple',
    };
    component.addDestination();
    expect(content.addDestination).toHaveBeenCalledWith(expect.objectContaining({ name: 'New temple' }));
    expect(component.destinationMessage()).toContain('public page');

    component.selectForm('place');
    component.destinationDraft = { ...component.destinationDraft, name: 'Heritage place', kind: 'Place', category: 'Heritage', imageUrl: 'https://example.test/place.jpg' };
    component.addDestination();
    expect(content.addDestination).toHaveBeenCalledTimes(2);
    expect(content.addDestination).toHaveBeenLastCalledWith(expect.objectContaining({ kind: 'Place' }));

    component.hotelDraft = {
      name: ' Guest house ',
      location: 'Ayodhya',
      description: 'Rooms',
      imageUrl: 'https://example.test/hotel.jpg',
      pricePerNight: 800,
      amenities: 'Family rooms',
    };
    component.addHotel();
    expect(hotels.addHotel).toHaveBeenCalledWith(expect.objectContaining({ name: 'Guest house' }));
    expect(component.hotelMessage()).toContain('Accommodation added');

    component.eventDraft = {
      title: 'Festival',
      date: '2026-12-01',
      location: 'Ayodhya',
      description: 'Festival',
      imageUrl: 'https://example.test/event.jpg',
    };
    component.addEvent();
    expect(content.addEvent).toHaveBeenCalledOnce();
    expect(component.eventMessage()).toContain('published');

    component.galleryImageUrls = 'https://example.test/image.jpg';
    component.galleryAlt = ' Temple ';
    component.galleryCategory = 'Ayodhya';
    component.addImage();
    expect(gallery.addMedia).toHaveBeenCalledWith(expect.objectContaining({
      src: 'https://example.test/image.jpg',
      alt: 'Temple',
      mediaType: 'IMAGE',
    }));
    expect(component.imageMessage()).toContain('public gallery');

    component.offerDraft = {
      title: 'Offer',
      description: 'Save on a trip',
      code: ' code10 ',
      discountPercent: 10,
      validUntil: '2026-12-31',
      tripId: 'trip-1',
      active: true,
      imageUrl: '',
    };
    component.addOffer();
    expect(offers.addOffer).toHaveBeenCalledWith(expect.objectContaining({ code: 'CODE10' }));
    expect(offers.addOffer.mock.calls[0][0]).not.toHaveProperty('imageUrl');
    expect(component.offerMessage()).toContain('published');

    const contactDraft = { phone: '+91 1234567890', email: 'hello@example.test', officeHours: 'Weekdays' };
    component.contactDraft = contactDraft;
    component.addContactDetails();
    expect(contacts.add).toHaveBeenCalledWith(contactDraft);
    expect(component.contactMessage()).toContain('saved');
  });

  it('does not report success or clear the draft when the API rejects a write', () => {
    gallery.addMedia.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
    component.galleryImageUrls = 'https://example.test/image.jpg';
    component.galleryAlt = 'Temple';
    component.galleryCategory = 'Ayodhya';

    component.addImage();

    expect(component.imageError()).toContain('403');
    expect(component.imageMessage()).toBe('');
    expect(component.galleryImageUrls).toBe('https://example.test/image.jpg');
  });

  it('saves several gallery sources sequentially and keeps unsaved sources after a failure', () => {
    component.galleryImageUrls = [
      'https://example.test/one.jpg',
      'https://example.test/two.jpg',
      'https://example.test/three.jpg',
    ].join('\n');
    component.galleryAlt = 'Pilgrimage';
    component.galleryCategory = 'Ayodhya';
    gallery.addMedia.mockImplementation((draft) => draft.src.endsWith('two.jpg')
      ? throwError(() => new HttpErrorResponse({ status: 503 }))
      : of({ ...draft, id: 'saved' }));

    component.addImage();

    expect(gallery.addMedia).toHaveBeenCalledTimes(2);
    expect(component.galleryImageUrls).toBe([
      'https://example.test/two.jpg',
      'https://example.test/three.jpg',
    ].join('\n'));
    expect(component.imageError()).toContain('503');
    expect(component.imageError()).toContain('1 item(s) were added');
  });

  it('accepts a YouTube video link as a separate gallery media record', () => {
    component.galleryVideoUrl = 'https://youtu.be/aqz-KE-bpKQ';
    component.galleryAlt = 'Short pilgrimage video';
    component.galleryCategory = 'Ayodhya';

    component.addImage();

    expect(gallery.addMedia).toHaveBeenCalledWith({
      src: 'https://youtu.be/aqz-KE-bpKQ',
      alt: 'Short pilgrimage video',
      category: 'Ayodhya',
      mediaType: 'VIDEO',
    });
    expect(component.galleryVideoUrl).toBe('');
  });

  it('uploads several selected local image files as separate gallery records', async () => {
    class TestFileReader {
      result: string | ArrayBuffer | null = null;
      onload: ((event: ProgressEvent<FileReader>) => unknown) | null = null;
      onerror: ((event: ProgressEvent<FileReader>) => unknown) | null = null;

      readAsDataURL(): void {
        this.result = 'data:image/png;base64,iVBORw0KGgo=';
        this.onload?.(new ProgressEvent('load') as ProgressEvent<FileReader>);
      }
    }
    vi.stubGlobal('FileReader', TestFileReader);
    component.selectedGalleryFiles.set([
      new File([new Uint8Array([137, 80, 78, 71])], 'one.png', { type: 'image/png' }),
      new File([new Uint8Array([137, 80, 78, 71])], 'two.png', { type: 'image/png' }),
    ]);
    component.galleryAlt = 'Pilgrimage images';
    component.galleryCategory = 'Ayodhya';

    component.addImage();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(gallery.addMedia).toHaveBeenCalledTimes(2);
    expect(gallery.addMedia.mock.calls[0][0]).toEqual(expect.objectContaining({
      src: expect.stringMatching(/^data:image\/png;base64,/),
      mediaType: 'IMAGE',
    }));
    expect(gallery.addMedia.mock.calls[1][0]).toEqual(expect.objectContaining({
      src: expect.stringMatching(/^data:image\/png;base64,/),
      mediaType: 'IMAGE',
    }));
    expect(component.selectedGalleryFiles()).toEqual([]);
    vi.unstubAllGlobals();
  });

  it('stops a gallery batch on logout without submitting later items', () => {
    const pending = new Subject<{ id: string }>();
    component.galleryImageUrls = 'https://example.test/one.jpg\nhttps://example.test/two.jpg';
    component.galleryAlt = 'Pilgrimage images';
    component.galleryCategory = 'Ayodhya';
    gallery.addMedia.mockReturnValue(pending);

    component.addImage();
    expect(gallery.addMedia).toHaveBeenCalledOnce();
    expect(component.savingGallery()).toBe(true);

    logoutEvents.next();

    expect(gallery.addMedia).toHaveBeenCalledOnce();
    expect(component.savingGallery()).toBe(false);
    expect(component.galleryImageUrls).toBe('https://example.test/one.jpg\nhttps://example.test/two.jpg');
  });

  it('reports remove success only after the backend confirms deletion', () => {
    component.removeEvent('event-1');
    expect(content.removeEvent).toHaveBeenCalledWith('event-1');
    expect(component.eventMessage()).toBe('Event removed.');

    hotels.removeHotel.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    component.removeHotel('hotel-1');
    expect(component.hotelError()).toContain('500');
    expect(component.hotelMessage()).toBe('');
  });
});
