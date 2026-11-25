import {CanActivateFn, Router} from '@angular/router';
import {inject} from "@angular/core";
import {AuthService} from "../services/auth.service";
import {catchError, map, of} from "rxjs";

export const adminOnlyGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // 1. Admin already known in memory: let through
  if (authService.isAdmin) {
    return true;
  }

  // 2. Otherwise (page reload), ask the server for the profile and its role
  return authService.checkSession().pipe(
    map(() => {
      // Check the role again once the session is known
      if (authService.isAdmin) {
        return true;
      }
      return router.parseUrl('/');
    }),
    catchError(() => {
      return of(router.parseUrl('/'));
    })
  );
};
