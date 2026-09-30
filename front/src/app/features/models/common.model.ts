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
