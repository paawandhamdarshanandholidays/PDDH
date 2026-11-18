import { booleanAttribute, Component, EventEmitter, forwardRef, Input, Output, signal } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
} from '@angular/forms';
import {
  isPublicHttpUrl,
  isSupportedImageDataUrl,
  MAX_IMAGE_FILE_SIZE_BYTES,
} from '../../../core/services/image-url.service';

const ACCEPTED_TYPES = new Set(['image/png', 'image/jpeg', 'image/gif', 'image/webp']);

@Component({
  selector: 'app-image-source-field',
  standalone: true,
  template: `
    <fieldset class="image-source-field">
      <legend>{{ label }}</legend>
      <div class="image-source-field__choices">
        <label><input type="radio" [checked]="mode === 'url'" (change)="setMode('url')"> Paste image URL</label>
        <label><input type="radio" [checked]="mode === 'upload'" (change)="setMode('upload')"> Browse and upload</label>
      </div>
      @if (mode === 'url') {
        <label class="image-source-field__input">Public image URL
          <input type="url" [value]="source" [disabled]="disabled" (input)="setUrl($event)" (blur)="markTouched(); urlBlur.emit()"
            autocomplete="url" placeholder="https://…" aria-label="Public image URL">
        </label>
      } @else {
        <label class="image-source-field__input">Choose an image
          <input type="file" [disabled]="disabled" accept="image/png,image/jpeg,image/gif,image/webp" (change)="readFile($event)"
            (blur)="markTouched()" aria-label="Choose an image file">
        </label>
        <p class="form-hint">PNG, JPEG, GIF, or WebP; maximum 5 MB.</p>
      }
      @if (source && previewable) {
        <div class="gallery-preview">
          <img [src]="source" [alt]="altText" (load)="previewLoaded($event)" (error)="previewErrored($event)">
          @if (previewState() === 'loading') { <span role="status">Checking image…</span> }
        </div>
      }
      @if (validationMessage()) { <p class="form-hint" role="alert">{{ validationMessage() }}</p> }
    </fieldset>
  `,
  styles: [`
    .image-source-field { border: 0; margin: 0; padding: 0; min-width: 0; }
    .image-source-field legend { font-weight: 600; margin-bottom: .5rem; }
    .image-source-field__choices { display: flex; flex-wrap: wrap; gap: 1rem; margin-bottom: .75rem; }
    .image-source-field__choices label { display: inline-flex; align-items: center; gap: .45rem; }
    .image-source-field__choices input { width: auto; }
    .image-source-field__input { display: grid; gap: .4rem; }
    .image-source-field .gallery-preview { margin-top: .75rem; }
    .image-source-field .gallery-preview img { max-height: 14rem; object-fit: contain; }
  `],
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => ImageSourceField), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => ImageSourceField), multi: true },
  ],
})
export class ImageSourceField implements ControlValueAccessor, Validator {
  @Input() label = 'Image';
  @Input() altText = 'Image preview';
  @Input({ transform: booleanAttribute }) required = true;
  @Output() readonly urlBlur = new EventEmitter<void>();
  readonly previewState = signal<'empty' | 'loading' | 'loaded' | 'unavailable' | 'invalid'>('empty');
  source = '';
  mode: 'url' | 'upload' = 'url';
  disabled = false;
  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};
  private onValidatorChange: () => void = () => {};

  get previewable(): boolean {
    return isPublicHttpUrl(this.source) || this.source.startsWith('data:image/');
  }

  writeValue(value: string | null): void {
    this.source = value ?? '';
    this.mode = this.source.startsWith('data:image/') ? 'upload' : 'url';
    this.previewState.set(this.previewable ? 'loading' : 'empty');
    this.onValidatorChange();
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(disabled: boolean): void {
    this.disabled = disabled;
  }

  validate(_control: AbstractControl): ValidationErrors | null {
    if (!this.source) return this.required ? { required: true } : null;
    if (this.source.startsWith('data:image/')) {
      if (!isSupportedImageDataUrl(this.source)) {
        return { invalidImageUpload: true };
      }
    } else if (!isPublicHttpUrl(this.source)) {
      return { invalidImageUrl: true };
    }
    if (this.previewState() === 'unavailable') return { imageUnavailable: true };
    return this.previewState() === 'loaded' ? null : { imageNotChecked: true };
  }

  registerOnValidatorChange(fn: () => void): void {
    this.onValidatorChange = fn;
  }

  setMode(mode: 'url' | 'upload'): void {
    if (this.mode === mode) return;
    this.mode = mode;
    this.setSource('');
    this.previewState.set('empty');
    this.onValidatorChange();
  }

  setUrl(event: Event): void {
    this.setSource((event.target as HTMLInputElement).value.trim());
    this.previewState.set(this.previewable ? 'loading' : 'invalid');
    this.onValidatorChange();
  }

  readFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (!ACCEPTED_TYPES.has(file.type) || file.size === 0 || file.size > MAX_IMAGE_FILE_SIZE_BYTES) {
      this.setSource('');
      this.previewState.set('invalid');
      input.value = '';
      this.onValidatorChange();
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result !== 'string' || !reader.result.startsWith(`data:${file.type};base64,`)) {
        this.setSource('');
        this.previewState.set('invalid');
      } else {
        this.setSource(reader.result);
        this.previewState.set('loading');
      }
      this.onValidatorChange();
    };
    reader.onerror = () => {
      this.setSource('');
      this.previewState.set('invalid');
      this.onValidatorChange();
    };
    reader.readAsDataURL(file);
  }

  previewLoaded(event: Event): void {
    if ((event.currentTarget as HTMLImageElement).getAttribute('src') !== this.source) return;
    this.previewState.set('loaded');
    this.onValidatorChange();
  }

  previewErrored(event: Event): void {
    if ((event.currentTarget as HTMLImageElement).getAttribute('src') !== this.source) return;
    this.previewState.set('unavailable');
    this.onValidatorChange();
  }

  markTouched(): void {
    this.onTouched();
  }

  validationMessage(): string {
    if (!this.source) return this.previewState() === 'invalid'
      ? 'The selected file or image URL is invalid.'
      : '';
    if (this.previewState() === 'invalid') return 'The selected file or image URL is invalid.';
    if (this.previewState() === 'unavailable') return 'The image could not be loaded. Check the URL or choose a valid image file.';
    return '';
  }

  private setSource(source: string): void {
    this.source = source;
    this.onChange(source);
    this.onTouched();
  }
}
