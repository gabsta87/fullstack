import { PhotoItem, Comment, VideoItem } from "./items.model";
import { GeographicZone, GeographicZoneWithParent } from "./filter.model";
import { CertificationRequest } from "./common.model";

export type UserRole = 'WORKER' | 'CLIENT' | 'ADMIN' | 'SUPER_ADMIN';
export type LanguageCode = 'EN' | 'FR' | 'IT' | 'DE' | 'ES';

export interface BaseUser {
  id: string;
  username: string;
  role: UserRole;
  email?: string;
  geographicZone: GeographicZone | null;
  language?: LanguageCode;
}

// ==========================================
// 1. WORKER PROFILES
// ==========================================

// Correspond à WorkerMinimalProfileDTO
export interface WorkerMinimalProfile extends BaseUser {
  age?: number;
  available: boolean;
  bodyType?: string;
  eyeColor?: string;
  hairColor?: string;
  isCertified: boolean;
  shortDescription: string;
  galleryIndex?: number;
  servicesId: number[];
  mainThumbUrl?: string;
  certifiedAt?: string;
}

// Correspond à WorkerPublicFullProfileDTO
export interface WorkerPublicFullProfile extends WorkerMinimalProfile {
  phone?: string;
  description?: string;
  photos: PhotoItem[];
  videos: VideoItem[];
  languages: string[];
  comments : Comment[];
}

// Correspond à WorkerFullProfileDTO (Profil privé du worker connecté)
export interface WorkerPrivateProfile extends WorkerPublicFullProfile {
  birthdate?: string;
  certificationRequest?: CertificationRequest;
  adminCertificationFeedback?: string;
  lastRefreshed?: string;
  expirationDate?: string;
}

// ==========================================
// 2. CLIENT & ADMIN ACCOUNTS
// ==========================================

// Correspond à ClientDTO
export interface ClientProfile extends BaseUser {
  favorites: WorkerMinimalProfile[];
}

// Correspond à AdminDTO
export interface AdminProfile extends BaseUser {}

// Correspond à AdminUserDTO (Pour le tableau de bord Admin)
export interface AdminUser {
  id: string;
  username: string;
  email: string;
  role: UserRole;
  language?: string;
  birthdate?: string;
  description?: string;
  phone?: string;
  available: boolean;
  banned: boolean;
  disabled: boolean;
  locked: boolean;
  certificationStatus?: string;
  verificationCode?: string;
  certifiedAt?: string;
  certificationRequestDate?: string;
  servicesId: number[];
  geographicZone: GeographicZoneWithParent | null;
}

// ==========================================
// 3. UPDATES
// ==========================================

export interface WorkerProfileUpdate {
  username?: string;
  description?: string;
  shortDescription?: string;
  geographicZoneId?: number;
  bodyType?: string;
  servicesId?: number[];
  eyeColor?: string;
  hairColor?: string;
  phone?: string;
  mainPhotoId?: string;
  birthdate?: string;
}
