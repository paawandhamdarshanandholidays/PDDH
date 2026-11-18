import { FormControl } from '@angular/forms';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { isImageSource } from '../../../core/services/image-url.service';
import { ImageSourceField } from './image-source-field';

describe('ImageSourceField', () => {
  let fixture: ComponentFixture<ImageSourceField>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ImageSourceField] }).compileComponents();
    fixture = TestBed.createComponent(ImageSourceField);
    fixture.detectChanges();
  });

  it('requires an accessible public URL or a valid uploaded image', () => {
    const component = fixture.componentInstance;
    component.writeValue('https://images.example.test/photo.png');
    expect(component.validate(new FormControl(''))).toEqual({ imageNotChecked: true });

    component.previewState.set('loaded');
    expect(component.validate(new FormControl(''))).toBeNull();

    component.previewState.set('unavailable');
    expect(component.validate(new FormControl(''))).toEqual({ imageUnavailable: true });
  });

  it('can be configured as optional without weakening required image fields', () => {
    const component = fixture.componentInstance;
    expect(component.validate(new FormControl(''))).toEqual({ required: true });

    component.required = false;
    expect(component.validate(new FormControl(''))).toBeNull();
  });

  it('rejects invalid data URLs and accepts the selected image formats', () => {
    const component = fixture.componentInstance;
    component.writeValue('data:image/svg+xml;base64,PHN2Zz4=');
    expect(component.validate(new FormControl(''))).toEqual({ invalidImageUpload: true });

    const uploadedImage = 'data:image/png;base64,iVBORw0KGgo=';
    expect(isImageSource(uploadedImage)).toBe(true);
    component.writeValue(uploadedImage);
    component.previewState.set('loaded');
    expect(component.validate(new FormControl(''))).toBeNull();
  });

  it('rejects malformed, empty, and oversized uploaded image data', () => {
    expect(isImageSource('data:image/png;base64,')).toBe(false);
    expect(isImageSource('data:image/png;base64,abc')).toBe(false);
    expect(isImageSource(`data:image/png;base64,${'A'.repeat(7_000_000)}`)).toBe(false);
  });
});
