export interface Offer {
  id: string;
  title: string;
  description: string;
  code: string;
  discountPercent: number;
  validUntil: string;
  tripId: string;
  active: boolean;
  imageUrl?: string;
}

export type OfferDraft = Omit<Offer, 'id'>;
