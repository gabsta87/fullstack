import { UserRole } from '../models/roles';

/**
 * Same rule as the backend (AdminController.requireAuthorityOver): an admin acts on clients and workers;
 * acting on an admin needs the super admin role; nobody acts on a super admin or on their own account.
 * Only hides buttons: the backend refuses the request anyway.
 */
export function canManageAccount(actor: { id?: string; role?: string } | null | undefined,
                                 target: { id?: string; role?: string }): boolean {
  if (!actor || actor.id === target.id) return false;
  if (target.role === UserRole.SUPER_ADMIN) return false;
  if (target.role === UserRole.ADMIN) return actor.role === UserRole.SUPER_ADMIN;
  return true;
}
