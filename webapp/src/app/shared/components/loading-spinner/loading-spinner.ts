import { Component, input } from '@angular/core';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  template: `<span class="spinner" role="status" [attr.aria-label]="label()"></span>`,
})
export class LoadingSpinner {
  readonly label = input('Loading');
}
