import {CanActivateFn, Router} from '@angular/router';
import {inject} from "@angular/core";
import {AuthService} from "../services/auth.service";
import {catchError, map, of} from "rxjs";

/** Lets the navigation through only when the server confirms the session. */
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Fast path: already known to be logged in.
  if (authService.isAuthenticated) {
    return true;
  }

  const denied = () => {
    authService.setRedirectUrl(state.url);
    return router.parseUrl('/');
  };

  // Slow path: ask the server. checkSession() emits false (it never errors) when there is no valid session.
  return authService.checkSession().pipe(
    map(isLoggedIn => isLoggedIn ? true : denied()),
    catchError(() => of(denied()))
  );
};
