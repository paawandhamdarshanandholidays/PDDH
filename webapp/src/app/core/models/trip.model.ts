export interface Trip {
  id: string;
  title: string;
  destination: string;
  summary: string;
  imageUrl: string;
  startDate: string;
  durationDays: number;
  price: number;
  category: 'Pilgrimage' | 'Heritage' | 'Nature';
  featured: boolean;
}

export interface TripDraft extends Omit<Trip, 'id'> {}
