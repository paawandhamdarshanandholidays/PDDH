export interface Hotel {
  id: string;
  name: string;
  location: string;
  description: string;
  imageUrl: string;
  pricePerNight: number;
  amenities: string;
}

export type HotelDraft = Omit<Hotel, 'id'>;
