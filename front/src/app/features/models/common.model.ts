import {BaseUser} from "./user.model";

export interface Service{
  id: number;
  name: string;
  description: string;
}

export interface LoginResponse {
  token: string;
  user: BaseUser;
}

export interface SpokenLanguage {
  language: string; // Matches the Language enum (e.g. 'FR', 'EN')
  level: number;    // Between 1 and 4
}

export const CertificationStatus = {
  NOT_CERTIFIED: 'NOT_CERTIFIED',
  PENDING_PHOTO: 'PENDING_PHOTO',
  PENDING_APPROVAL: 'PENDING_APPROVAL',
  NEEDS_REVISION: 'NEEDS_REVISION',
  CERTIFIED: 'CERTIFIED',
  REJECTED: 'REJECTED',
} as const;

export type CertificationStatus = typeof CertificationStatus[keyof typeof CertificationStatus];

export interface CertificationRequest {
  id: number;
  workerId: string;
  workerUsername: string;
  verificationCode: string;
  certificationPhotoUrl?: string;
  status: CertificationStatus;
  underReview: boolean;
  lockedByAdminId?: number;
  createdAt: string;
  processedAt: string;
  lockedAt?: string;
  comment?: string;
}

export interface AdminLog {
  id: number;
  adminId: string;
  adminName: string;
  actionType: string;
  details?: string;
  /** Role of the user (WORKER, CLIENT…), SERVICE, REGION, LEGAL_TEXT or SETTINGS; absent for deleted items (see targetSnapshot) */
  targetType?: string | null;
  targetId?: string | null;
  targetLabel?: string | null;
  /** Email of the targeted account */
  targetEmail?: string | null;
  /** JSON snapshot of a deleted item (ids, name, excerpt) */
  targetSnapshot?: string | null;
  createdAt: string;
}

/** A setting the admins can change (see backend SettingKey). */
export interface AdminSetting {
  key: 'CERTIFICATION_VALIDITY_MONTHS' | 'CERTIFICATION_RENEWAL_MONTHS' | 'STORAGE_LIMIT_MB';
  value: number;
  defaultValue: number;
  min: number;
  max: number;
}
