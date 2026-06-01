import {CanActivateFn, Router} from '@angular/router';
import {inject} from "@angular/core";
import {AuthService} from "../services/auth.service";
import {catchError, map, of} from "rxjs";

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // 1. "Fast Path" : Si on sait localement qu'on est connecté, on autorise immédiatement
  if (authService.isAuthenticated) {
    return true;
  }

  // 2. "Slow Path" : Appel serveur pour vérifier la session
  return authService.checkSession().pipe(
    map(isAuth => {
      if (isAuth) return true;

      // Si pas authentifié, on mémorise l'URL et on redirige
      authService.setRedirectUrl(state.url);
      router.navigate(['/login']);
      return false;
    }),
    catchError((err) => {
      // 3. Gestion d'erreur (réseau, timeout, 500...)
      // On considère une erreur comme "non authentifié"
      console.error("authGuard: Erreur lors de la vérification de session", err);

      authService.setRedirectUrl(state.url);
      router.navigate(['/login']);
      return of(false);
    })
  );
};
