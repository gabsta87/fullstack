import { certificationState } from './certification-state';

describe('certificationState', () => {
  const now = new Date('2026-11-26T12:00:00');
  const at = (days: number) => new Date(now.getTime() + days * 86_400_000).toISOString();
  const certified = (status: string, expiresInDays: number, renewableInDays: number) => ({
    isCertified: true, certificationExpiresAt: at(expiresInDays), certificationRenewableFrom: at(renewableInDays),
    certificationRequest: { status } });

  it('follows the request before any certification', () => {
    expect(certificationState({}, now)).toBe('none');
    expect(certificationState({ certificationRequest: { status: 'NOT_CERTIFIED' } }, now)).toBe('none');
    expect(certificationState({ certificationRequest: { status: 'PENDING_PHOTO' } }, now)).toBe('pending_photo');
    expect(certificationState({ certificationRequest: { status: 'PENDING_APPROVAL' } }, now)).toBe('pending_approval');
    expect(certificationState({ certificationRequest: { status: 'REJECTED' } }, now)).toBe('revision');
    expect(certificationState({ certificationRequest: { status: 'NEEDS_REVISION' } }, now)).toBe('revision');
  });

  it('uses the dates sent by the server for a certification', () => {
    expect(certificationState(certified('CERTIFIED', 120, 60), now)).toBe('valid');
    expect(certificationState(certified('CERTIFIED', 30, -30), now)).toBe('renewal_open');
    expect(certificationState(certified('PENDING_PHOTO', 30, -30), now)).toBe('renewal_open');
    expect(certificationState(certified('PENDING_APPROVAL', 30, -30), now)).toBe('renewal_pending');
  });

  it('expires at the end date, even on a page left open', () => {
    expect(certificationState({ ...certified('CERTIFIED', -1, -60), isCertified: false }, now)).toBe('expired');
    expect(certificationState(certified('CERTIFIED', -1, -60), now)).toBe('expired'); // server said true earlier
    expect(certificationState({ ...certified('PENDING_PHOTO', -1, -60), isCertified: false }, now)).toBe('expired');
  });
});
