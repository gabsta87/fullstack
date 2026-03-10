import {CanActivateFn, Router} from '@angular/router';
import {inject} from "@angular/core";
import {AuthService} from "../services/auth.service";
import {map} from "rxjs";

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.checkSession().pipe(
    map(isAuth => {
      if (isAuth) return true;
      authService.setRedirectUrl(state.url);
      router.navigate(['/login']);
      return false;
    })
  );
};
