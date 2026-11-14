import { Component, input } from '@angular/core';
import { GalleryImage } from '../../../core/models/content.model';

@Component({
  selector: 'app-image-gallery',
  standalone: true,
  template: `
    <div class="gallery-grid">
      @for (image of images(); track image.id) {
        <figure class="gallery-tile">
          <img [src]="image.src" [alt]="image.alt" loading="lazy">
          <figcaption>{{ image.category }}</figcaption>
        </figure>
      }
    </div>
  `,
})
export class ImageGallery {
  readonly images = input.required<GalleryImage[]>();
}
