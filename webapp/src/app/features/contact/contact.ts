import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EnquiryService } from '../../core/services/enquiry.service';
import { ContactDetailsService } from '../../core/services/contact-details.service';
import { IdempotencySubmission } from '../../core/services/idempotency-key';

@Component({
  selector: 'app-contact',
  imports: [FormsModule],
  templateUrl: './contact.html',
  styleUrl: './contact.css',
})
export class Contact {
  readonly sent = signal(false);
  readonly error = signal('');
  readonly submittedName = signal('');
  readonly submitting = signal(false);
  readonly contactDetails = inject(ContactDetailsService);
  private readonly enquiries = inject(EnquiryService);
  private readonly submission = new IdempotencySubmission();
  name = '';
  email = '';
  phone = '';
  subject = '';
  message = '';

  phoneLink(phone: string): string {
    return phone.replace(/[\s()-]/g, '');
  }

  submit(): void {
    if (!this.name.trim() || !this.email.trim() || !this.phone.trim() || !this.message.trim()) {
      this.error.set('Please complete every field before sending your message.');
      return;
    }
    this.error.set('');
    this.sent.set(false);
    this.submitting.set(true);
    const request = {
      name: this.name.trim(),
      email: this.email.trim(),
      phone: this.phone.trim(),
      subject: this.subject.trim() || 'Website enquiry',
      message: this.message.trim(),
    };
    this.enquiries.submit(request, this.submission.forPayload(request)).subscribe({
      next: () => {
        this.submittedName.set(this.name.trim());
        this.sent.set(true);
        this.name = '';
        this.email = '';
        this.phone = '';
        this.subject = '';
        this.message = '';
        this.submitting.set(false);
        this.submission.clear();
      },
      error: (error: unknown) => {
        console.error('Could not submit the contact enquiry.', error);
        this.error.set('We could not send your message right now. Please call +91 78973 21190 and try again later.');
        this.submitting.set(false);
      },
    });
  }
}
