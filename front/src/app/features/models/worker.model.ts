// src/app/features/models/worker.model.ts

export const REGIONS = ['Paris','Lyon','Marseille','Bordeaux','Toulouse','Nice','Nantes','Strasbourg'] as const;
export const BODY_TYPES = ['Mince','Athlétique','Normale','Pulpeuse','Ronde'] as const;
export const SERVICES = ['Standard','Premium'] as const;

export interface ColorOption {
  label: string;
  hex: string;
}

export const EYE_COLORS: ColorOption[] = [
  { label: 'Marron',   hex: '#6b3d2e' }, { label: 'Noisette', hex: '#9b6a3a' },
  { label: 'Vert',     hex: '#4a7c59' }, { label: 'Bleu',     hex: '#4a7aaf' },
  { label: 'Gris',     hex: '#8a9aaa' },
];

export const HAIR_COLORS: ColorOption[] = [
  { label: 'Noir',    hex: '#1a1410' }, { label: 'Brun',    hex: '#5c3d2e' },
  { label: 'Châtain', hex: '#8b5e3c' }, { label: 'Blond',   hex: '#d4a847' },
  { label: 'Roux',    hex: '#c04a1a' },
];

// --- Interfaces existantes ---

export interface WorkerGalleryDTO {
  id: string;
  name: string;
  age: number;
  location: string;
  region: string;
  bodyType: string;
  height: number;
  weight: number;
  services: string[];
  available: boolean;
  lastRefreshed: string;
  mainThumbUrl: string | null;
  previewThumbUrls: string[];
}

export interface WorkerProfile extends WorkerGalleryDTO {
  eyeColor: string;
  hairColor: string;
  serviceList: ServiceItem[];
  responseTime: string;
  phone: string;
  rating: number;
  reviewCount: number;
  description: string;
  tags: string[];
  photos: PhotoItem[];
  videos: VideoItem[];
  reviews: Review[];
}

export interface ServiceItem  { name: string; price: string; }
export interface PhotoItem    { id: string; originalUrl: string; mainThumbUrl: string; previewThumbUrl: string; }
export interface VideoItem    { id: string; url: string; duration?: string; }
export interface Review       { author: string; authorInitial: string; rating: number; date: string; text: string; }

// --- Filtres typés ---

export interface GalleryFilters {
  region?: string;
  bodyType?: string[]; // Changé en tableau pour correspondre à la logique de sélection multiple
  services?: string[];
  eyeColor?: string;
  hairColor?: string;
  heightMin?: number | null;
  heightMax?: number | null;
  weightMin?: number | null;
  weightMax?: number | null;
}
