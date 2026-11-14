export class IdempotencySubmission {
  private payloadFingerprint = '';
  private key: string | null = null;

  forPayload(payload: unknown): string {
    const fingerprint = JSON.stringify(payload);
    if (this.key === null || fingerprint !== this.payloadFingerprint) {
      this.payloadFingerprint = fingerprint;
      this.key = createIdempotencyKey();
    }
    return this.key;
  }

  clear(): void {
    this.payloadFingerprint = '';
    this.key = null;
  }
}

export function createIdempotencyKey(): string {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();
  if (globalThis.crypto?.getRandomValues) {
    const bytes = globalThis.crypto.getRandomValues(new Uint8Array(16));
    return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');
  }
  throw new Error('Secure random generation is required to submit this request.');
}
