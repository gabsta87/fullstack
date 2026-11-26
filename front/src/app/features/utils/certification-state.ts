import { CertificationStatus } from '../models/common.model';

/**
 * Where a worker stands with the certification. The dates come from the server (fixed at approval):
 * nothing is computed from a hard-coded validity here.
 */
export type CertificationState =
  | 'none'             // never certified, or revoked: can start a request
  | 'pending_photo'    // request opened, photo to send
  | 'pending_approval' // photo sent, admins have to review it
  | 'revision'         // refused or correction requested
  | 'valid'            // certified, renewal not open yet
  | 'renewal_open'     // certified, may send a new photo now
  | 'renewal_pending'  // certified, new photo waiting for the admins
  | 'expired';         // the end date has passed

export interface CertificationInfo {
  isCertified?: boolean;
  certificationExpiresAt?: string | null;
  certificationRenewableFrom?: string | null;
  certificationRequest?: { status?: string } | null;
}

export function certificationState(me: CertificationInfo, now: Date = new Date()): CertificationState {
  const status = me.certificationRequest?.status;
  const expiresAt = me.certificationExpiresAt ? new Date(me.certificationExpiresAt) : null;
  const certified = !!me.isCertified && (!expiresAt || expiresAt > now);

  if (certified) {
    if (status === CertificationStatus.PENDING_APPROVAL) return 'renewal_pending';
    const renewableFrom = me.certificationRenewableFrom ? new Date(me.certificationRenewableFrom) : null;
    return renewableFrom && renewableFrom <= now ? 'renewal_open' : 'valid';
  }
  if (status === CertificationStatus.NEEDS_REVISION || status === CertificationStatus.REJECTED) return 'revision';
  if (status === CertificationStatus.PENDING_APPROVAL) return 'pending_approval';
  if (status === CertificationStatus.CERTIFIED || (expiresAt && expiresAt <= now)) return 'expired';
  if (status === CertificationStatus.PENDING_PHOTO) return 'pending_photo';
  return 'none';
}
