export interface Service{
  id: number;
  name: string;
  description: string;
}

export const CertificationStatus = {
  NOT_CERTIFIED: 'NOT_CERTIFIED',
  PENDING_PHOTO: 'PENDING_PHOTO',
  PENDING_APPROVAL: 'PENDING_APPROVAL',
  NEEDS_REVISION: 'NEEDS_REVISION',
  APPROVED: 'APPROVED',
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
  processedAt?: string;
  lockedAt?: string;
  comment?: string;
}

export interface AdminLog {
  id: number;
  adminId: number;
  actionType: string;
  targetSnapshot?: string;
  details?: string;
  createdAt: string;
}
