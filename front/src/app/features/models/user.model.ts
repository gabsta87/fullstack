import { PhotoItem, Comment, VideoItem } from "./items.model";
import { GeographicZone, GeographicZoneWithParent } from "./filter.model";
import {CertificationRequest, SpokenLanguage} from "./common.model";

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

// Matches WorkerMinimalProfileDTO
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

// Matches WorkerPublicFullProfileDTO
export interface WorkerPublicFullProfile extends WorkerMinimalProfile {
  banned: boolean;
  locked: boolean;
  phone?: string;
  description?: string;
  photos: PhotoItem[];
  videos: VideoItem[];
  languages: string[];
  comments : Comment[];
  spokenLanguages: SpokenLanguage[];
}

// Matches WorkerFullProfileDTO (private profile of the logged-in worker)
export interface WorkerPrivateProfile extends WorkerPublicFullProfile {
  birthdate?: string;
  certificationRequest?: CertificationRequest;
  /** End of the current certification and start of its renewal period (set by the server at approval). */
  certificationExpiresAt?: string | null;
  certificationRenewableFrom?: string | null;
  adminCertificationFeedback?: string;
  lastRefreshed?: string;
  expirationDate?: string;
  hidden : boolean;
  spokenLanguages: SpokenLanguage[];
  reviews: Comment[];
}

// ==========================================
// 2. CLIENT & ADMIN ACCOUNTS
// ==========================================

// Matches ClientDTO
export interface ClientProfile extends BaseUser {
  favorites: WorkerMinimalProfile[];
  reviews: Comment[];
}

// Matches AdminDTO
export interface AdminProfile extends BaseUser {}

// Matches AdminUserDTO (admin dashboard)
export interface AdminUser {
  id: string;
  username: string;
  email: string;
  role: UserRole;
  language?: string;
  birthdate?: string;
  phone?: string;
  available: boolean;
  banned: boolean;
  locked: boolean;
  hidden : boolean;
  certificationStatus?: string;
  verificationCode?: string;
  certifiedAt?: string;
  /** Valid certification right now (certificationStatus may be a renewal in progress meanwhile). */
  certified?: boolean;
  certificationExpiresAt?: string | null;
  certificationRequestDate?: string;
  description?: string;
  servicesId: number[];
  geographicZone: GeographicZoneWithParent | null;
  /** Why a worker profile is not public: USERNAME, PHONE, PHOTOS…, MINOR, BANNED, LOCKED, HIDDEN, EXPIRED. */
  profileIssues?: string[];
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
