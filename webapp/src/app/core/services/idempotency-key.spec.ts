import { describe, expect, it } from 'vitest';
import { IdempotencySubmission } from './idempotency-key';

describe('IdempotencySubmission', () => {
  it('reuses a key for the same payload and rotates it when the payload changes', () => {
    const submission = new IdempotencySubmission();
    const firstPayload = { name: 'Pawan', email: 'pawan@example.com' };

    const firstKey = submission.forPayload(firstPayload);
    expect(submission.forPayload({ ...firstPayload })).toBe(firstKey);
    expect(submission.forPayload({ ...firstPayload, name: 'Pawan Dham' })).not.toBe(firstKey);
  });

  it('generates a new key after the successful submission is cleared', () => {
    const submission = new IdempotencySubmission();
    const payload = { bookingType: 'TRIP', guests: 2 };
    const firstKey = submission.forPayload(payload);

    submission.clear();

    expect(submission.forPayload(payload)).not.toBe(firstKey);
  });
});
