export interface Offer {
  id: string;
  title: string;
  description: string;
  code: string;
  discountPercent: number;
  validUntil: string;
  tripId: string;
  active: boolean;
}

export type OfferDraft = Omit<Offer, 'id'>;
