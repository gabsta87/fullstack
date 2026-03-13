// src/app/features/models/worker.model.ts

export interface WorkerGalleryDTO {
  id: string;              // UUID string — matches Java UUID serialised by Jackson
  name: string;
  age: number;
  location: string;
  region: string;
  bodyType: string;
  height: number;
  weight: number;
  services: string[];
  available: boolean;
  lastRefreshed: string;   // ISO-8601 from backend
  mainThumbUrl: string | null;
  previewThumbUrls: string[]; // empty on gallery load — fetched lazily on hover
}

export interface WorkerProfile {
  id: string;              // UUID string
  name: string;
  age: number;
  location: string;
  region: string;
  bodyType: string;
  eyeColor: string;
  hairColor: string;
  height: number;
  weight: number;
  services: string[];
  serviceList: ServiceItem[];
  available: boolean;
  responseTime: string;
  phone: string;
  rating: number;
  reviewCount: number;
  description: string;
  tags: string[];
  mainThumbUrl: string | null;
  photos: PhotoItem[];
  videos: VideoItem[];
  reviews: Review[];
}

export interface ServiceItem  { name: string; price: string; }
export interface PhotoItem    { id: string; originalUrl: string; mainThumbUrl: string; previewThumbUrl: string; }
export interface VideoItem    { id: string; url: string; duration?: string; }
export interface Review       { author: string; authorInitial: string; rating: number; date: string; text: string; }

export interface GalleryFilters {
  region?: string;
  bodyType?: string[];
  eyeColor?: string;
  hairColor?: string;
  heightMin?: number;
  heightMax?: number;
  weightMin?: number;
  weightMax?: number;
  services?: string[];
}
