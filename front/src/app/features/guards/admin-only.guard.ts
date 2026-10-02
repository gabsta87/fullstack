import {CanActivateFn, Router} from '@angular/router';
import {inject} from "@angular/core";
import {AuthService} from "../services/auth.service";

export const adminOnlyGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAdmin) {
    return true;
  }

  const tokenData = authService.getDecodedToken();
  const role = tokenData?.role;

  // 💡 On vérifie avec et sans le préfixe 'ROLE_' pour éviter les faux négatifs au F5
  if (role === 'ADMIN' || role === 'SUPER_ADMIN' || role === 'ROLE_ADMIN' || role === 'ROLE_SUPER_ADMIN') {
    return true;
  }

  return router.parseUrl('/');
};
