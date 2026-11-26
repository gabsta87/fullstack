import { canManageAccount } from './admin-rights';

describe('canManageAccount', () => {
  const admin = { id: 'a1', role: 'ADMIN' };
  const superAdmin = { id: 's1', role: 'SUPER_ADMIN' };

  it('lets an admin manage clients and workers', () => {
    expect(canManageAccount(admin, { id: 'c', role: 'CLIENT' })).toBeTrue();
    expect(canManageAccount(admin, { id: 'w', role: 'WORKER' })).toBeTrue();
  });

  it('keeps admins away from other admins and super admins', () => {
    expect(canManageAccount(admin, { id: 'a2', role: 'ADMIN' })).toBeFalse();
    expect(canManageAccount(admin, superAdmin)).toBeFalse();
  });

  it('lets a super admin manage admins, but not another super admin or themself', () => {
    expect(canManageAccount(superAdmin, admin)).toBeTrue();
    expect(canManageAccount(superAdmin, { id: 's2', role: 'SUPER_ADMIN' })).toBeFalse();
    expect(canManageAccount(superAdmin, superAdmin)).toBeFalse();
    expect(canManageAccount(admin, admin)).toBeFalse();
  });

  it('refuses when the current account is unknown', () => {
    expect(canManageAccount(null, { id: 'c', role: 'CLIENT' })).toBeFalse();
  });
});
