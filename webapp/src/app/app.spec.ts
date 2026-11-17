import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter(routes)],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the site navigation and footer', async () => {
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    for (const resource of ['temples', 'places', 'events', 'gallery', 'hotels', 'offers', 'trips', 'contact']) {
      http.expectOne(`http://localhost:9091/api/catalog/${resource}`).flush([]);
    }
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.brand strong')?.textContent).toContain('Pawan Dham');
    expect(compiled.querySelector('.site-footer')?.textContent).toContain('Darshan & Holidays');
    http.verify();
  });
});
