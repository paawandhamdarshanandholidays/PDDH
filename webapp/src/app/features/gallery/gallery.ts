import { Component, computed, inject, signal } from '@angular/core';
import { GalleryService } from '../../core/services/gallery.service';
import { ImageGallery } from '../../shared/components/image-gallery/image-gallery';

@Component({
  selector: 'app-gallery',
  imports: [ImageGallery],
  templateUrl: './gallery.html',
  styleUrl: './gallery.css',
})
export class Gallery {
  private readonly gallery = inject(GalleryService);
  readonly category = signal('All');
  readonly categories = this.gallery.categories;
  readonly filteredImages = computed(() => this.category() === 'All'
    ? this.gallery.images()
    : this.gallery.images().filter((image) => image.category === this.category()));

}
