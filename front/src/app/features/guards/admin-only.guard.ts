import {CanActivateFn, Router} from '@angular/router';
import {inject} from "@angular/core";
import {AuthService} from "../services/auth.service";
import {catchError, map, of} from "rxjs";

export const adminOnlyGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // 1. Si l'utilisateur est déjà en mémoire et qu'il est admin, on laisse passer instantanément
  if (authService.isAdmin) {
    return true;
  }

  // 2. Sinon (cas du F5), on interroge le serveur pour récupérer le profil et son rôle
  return authService.checkSession().pipe(
    map(() => {
      // Une fois la session vérifiée, on re-teste si c'est un admin
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
