import {CanActivateFn, Router} from '@angular/router';
import {inject} from "@angular/core";
import {AuthService} from "../services/auth.service";
import {firstValueFrom} from "rxjs";

export const accountRedirectGuard: CanActivateFn = async () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const isAuthenticated = await firstValueFrom(authService.isAuthenticated$);

  if (!isAuthenticated) {
    // Si pas connecté, on redirige vers l'accueil (ou on ouvre la modale)
    return router.parseUrl('/');
  }

  const user = authService.getUser();
  const role = user?.role;

  // Aiguillage propre centralisé
  if (role === 'ADMIN' || role === 'SUPER_ADMIN') {
    return router.parseUrl('/admin-dashboard');
  } else if (role === 'WORKER') {
    return router.parseUrl('/profile-management');
  } else if (role === 'CLIENT') {
    return router.parseUrl('/account');
  }

  // Par sécurité si aucun rôle ne matche
  return router.parseUrl('/');
};
