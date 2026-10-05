import {PhotoItem, Review, VideoItem} from "./items.model";
import {GeographicZone, GeographicZoneWithParent} from "./filter.model";
import {CertificationRequest} from "./common.model";

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
  verificationCode?: string;
  adminCertificationFeedback?: string;
  certificationRequest : CertificationRequest;
  isCertified : boolean;
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
  servicesId: number[];
  certificationStatus: string;
  certifiedAt: string;
}

export interface WorkerFullProfile extends WorkerSimpleProfile {
  description: string;
  mainThumbUrl: string;
  phone: string;
  age : number
  certificationPhotoUrl : string;

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
  servicesId?: number[];
  eyeColor?: string;
  hairColor?: string;
  phone?: string;
  mainPhotoId?: string;
  birthdate?: string;
}

export interface WorkerProfileForAdmin{
  id: string;
  available: boolean;
  banned : boolean;
  disabled: boolean;
  locked: boolean;
  role : 'WORKER' | 'CLIENT' | 'ADMIN' | 'SUPER_ADMIN';
  username: string;
  email: string;
  language : 'EN' | 'FR' | 'IT' | 'DE' | 'ES';
  birthdate: string;
  description: string;
  phone: string;
  certificationStatus: string;
  verificationCode?: string;
  certifiedAt : string,
  certificationRequestDate : string,
  servicesId: number[];
  geographicZone: GeographicZoneWithParent | null;
}


export interface UserSimpleForAdmin {
  id: string;
  username: string;
  email: string;
  role: string;
  locked: boolean;
  certified: boolean;
}
