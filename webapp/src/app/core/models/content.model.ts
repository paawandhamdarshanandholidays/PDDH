export interface Destination {
  id: string;
  kind: 'Temple' | 'Place';
  name: string;
  region: string;
  description: string;
  imageUrl: string;
  category: 'Temple' | 'Nature' | 'Heritage';
}

export interface Event {
  id: string;
  title: string;
  date: string;
  location: string;
  description: string;
  imageUrl: string;
}

export interface GalleryImage {
  id: string;
  src: string;
  alt: string;
  category: string;
}
