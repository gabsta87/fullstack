import {PhotoItem, Review, VideoItem} from "./items.model";
import {GeographicZone} from "./filter.model";

export interface BaseUser {
  id: string;
  username: string;
  role: 'WORKER' | 'CLIENT' | 'ADMIN' | 'SUPER_ADMIN';
  geographicZone: GeographicZone | null;
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
  certificationStatus: string;
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
  username?: string;
  description?: string;
  geographicZoneId?: number;
  bodyType?: string;
  services?: string[];
  eyeColor?: string;
  hairColor?: string;
  phone?: string;
  mainPhotoId?: string;
  birthdate?: string;
}

export interface WorkerProfileForAdmin{
  id: string;
  disabled: boolean;
  username: string;
  geographicZone: GeographicZone | null;
  email: string;
  language : 'EN' | 'FR' | 'IT' | 'DE' | 'ES';
  lastRefreshed: string;
  expirationDate: string;
  birthdate: string;
  available: boolean;
  banned : boolean;
  services: string[];
  description: string;
  phone: string;
  age : number
  certificationStatus: string;
}
