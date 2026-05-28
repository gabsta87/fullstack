import {PhotoItem, Review, VideoItem} from "./items.model";

export interface BaseUser {
  id: string;
  username: string;
  role: 'WORKER' | 'CLIENT' | 'ADMIN';
  location: string;
  region : string;
}

// PRIVATE DATA

export interface PrivateAccount{
  email: string;
  language : 'EN' | 'FR' | 'IT' | 'DE' | 'ES';
}

export interface WorkerPrivateAccount extends PrivateAccount, WorkerFullProfile {
  lastRefreshed: string;
  expirationDate: string;
  birthdate: string;
}

export interface ClientPrivateAccount extends PrivateAccount, BaseUser{
  favorites: WorkerSimpleProfile[];
}

// PUBLIC DATA

export interface WorkerSimpleProfile extends BaseUser{
  available: boolean;
  bodyType: string;
  eyeColor: string;
  hairColor: string;
  mainThumbUrl: string;
  previewThumbUrls: string[];
  services: string[];
}

export interface WorkerFullProfile extends WorkerSimpleProfile {
  description: string;
  mainThumbUrl: string;
  phone: string;
  age : number

  photos: PhotoItem[];
  videos: VideoItem[];
  reviews: Review[];
}

// UPDATES

export interface WorkerProfileUpdate {
  description?: string;
  location?: string;
  bodyType?: string;
  services?: string[];
  eyeColor?: string;
  hairColor?: string;
  phone?: string;
  mainPhotoId?: string;
}

