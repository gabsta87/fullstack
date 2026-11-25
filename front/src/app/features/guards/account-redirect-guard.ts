import {CanActivateFn, Router} from '@angular/router';
import {inject} from "@angular/core";
import {AuthService} from "../services/auth.service";
import {firstValueFrom} from "rxjs";
import {UserRole} from "../models/roles";

export const accountRedirectGuard: CanActivateFn = async () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const isAuthenticated = await firstValueFrom(authService.isAuthenticated$);

  if (!isAuthenticated) {
    // Not logged in: go home
    return router.parseUrl('/');
  }

  const user = authService.getUser();
  const role = user?.role;

  // Route by role
  if (role === UserRole.ADMIN || role === UserRole.SUPER_ADMIN) {
    return router.parseUrl('/admin-dashboard');
  } else if (role === UserRole.WORKER) {
    return router.parseUrl('/profile-management');
  } else if (role === UserRole.CLIENT) {
    return router.parseUrl('/account');
  }

  // No known role: go home
  return router.parseUrl('/');
};
