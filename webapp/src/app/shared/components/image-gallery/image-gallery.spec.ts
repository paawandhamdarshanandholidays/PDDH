import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ImageGallery } from './image-gallery';

describe('ImageGallery', () => {
  let fixture: ComponentFixture<ImageGallery>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ImageGallery] }).compileComponents();
    fixture = TestBed.createComponent(ImageGallery);
  });

  it('continues to render legacy records as images and embeds recognized YouTube videos', () => {
    fixture.componentRef.setInput('images', [
      { id: 'legacy', src: 'https://example.test/photo.jpg', alt: 'Temple', category: 'Ayodhya' },
      {
        id: 'video',
        src: 'https://youtu.be/aqz-KE-bpKQ',
        alt: 'Pilgrimage video',
        category: 'Ayodhya',
        mediaType: 'VIDEO',
      },
    ]);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('img')?.getAttribute('alt')).toBe('Temple');
    expect(element.querySelector('iframe')?.getAttribute('title')).toBe('Pilgrimage video');
    expect(element.querySelector('video')).toBeNull();
  });

  it('uses a native player and source link for non-YouTube video URLs', () => {
    fixture.componentRef.setInput('images', [{
      id: 'video',
      src: 'https://cdn.example.test/clip.mp4',
      alt: 'Pilgrimage video',
      category: 'Ayodhya',
      mediaType: 'VIDEO',
    }]);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('video')?.getAttribute('src')).toBe('https://cdn.example.test/clip.mp4');
    expect(element.querySelector('a')?.getAttribute('href')).toBe('https://cdn.example.test/clip.mp4');
  });
});
