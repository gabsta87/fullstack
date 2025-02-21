import { CanActivateFn } from '@angular/router';

export const loggedAsAdminGuard: CanActivateFn = (route, state) => {
  // TODO return TRUE if the user is logged as an admin, FALSE otherwise
  return true;
};
