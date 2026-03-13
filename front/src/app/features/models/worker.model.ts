// This class is used in the gallery, to have minimal information on all profiles
export class Profile{
  id!: number;
  pseudo!:string;
  description!:string;
  phone!:string;
  address!:string;
  comments!:string[];
  mainPhoto!:string;
}

export class ProfileDetail extends Profile{
  photos!:string[];
}

export interface WorkerGalleryDTO {
  id: number;
  name: string;
  age: number;
  location: string;
  region: string;
  bodyType: string;
  height: number;
  services: string[];
  available: boolean;
  lastRefreshed: string; // ISO string from backend
  mainThumbUrl: string | null;
  previewThumbUrls: string[]; // populated lazily on hover
}

export interface WorkerProfile {
  id: number;
  name: string;
  age: number;
  location: string;
  region: string;
  bodyType: string;
  eyeColor: string;
  hairColor: string;
  height: number;
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

export interface ServiceItem {
  name: string;
  price: string;
}

export interface PhotoItem {
  id: number;
  originalUrl: string;
  mainThumbUrl: string;
  previewThumbUrl: string;
  isMain: boolean;
}

export interface VideoItem {
  id: number;
  url: string;
  duration?: string;
}

export interface Review {
  author: string;
  authorInitial: string;
  rating: number;
  date: string;
  text: string;
}

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
