import { CanActivateFn } from '@angular/router';

export const loggedGuard: CanActivateFn = (route, state) => {
  // TODO return TRUE if the user is logged, FALSE otherwise
  return true;
};
