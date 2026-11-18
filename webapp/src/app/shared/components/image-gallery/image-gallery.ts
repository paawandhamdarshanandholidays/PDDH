import { Component, inject, input } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { GalleryMedia } from '../../../core/models/content.model';
import { getYouTubeVideoId } from '../../../core/services/gallery-video-url';

@Component({
  selector: 'app-image-gallery',
  standalone: true,
  template: `
    <div class="gallery-grid">
      @for (image of images(); track image.id) {
        <figure class="gallery-tile">
          @if (image.mediaType === 'VIDEO') {
            @if (youtubeEmbedUrl(image.src); as embedUrl) {
              <iframe [src]="embedUrl" [title]="image.alt" loading="lazy"
                referrerpolicy="strict-origin-when-cross-origin"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                allowfullscreen></iframe>
            } @else {
              <video [src]="image.src" [attr.aria-label]="image.alt" controls playsinline preload="metadata"></video>
              <a class="gallery-video-link" [href]="image.src" target="_blank" rel="noopener noreferrer">Open video in a new tab ↗</a>
            }
          } @else {
            <img [src]="image.src" [alt]="image.alt" loading="lazy">
          }
          <figcaption>{{ image.category }}</figcaption>
        </figure>
      }
    </div>
  `,
})
export class ImageGallery {
  readonly images = input.required<GalleryMedia[]>();
  private readonly sanitizer = inject(DomSanitizer);
  private readonly embedUrls = new Map<string, SafeResourceUrl>();

  youtubeEmbedUrl(source: string): SafeResourceUrl | null {
    const id = getYouTubeVideoId(source);
    if (!id) return null;
    let embedUrl = this.embedUrls.get(id);
    if (!embedUrl) {
      embedUrl = this.sanitizer.bypassSecurityTrustResourceUrl(
        `https://www.youtube-nocookie.com/embed/${id}?rel=0`,
      );
      this.embedUrls.set(id, embedUrl);
    }
    return embedUrl;
  }
}
