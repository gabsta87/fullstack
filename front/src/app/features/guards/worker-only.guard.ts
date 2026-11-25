import {CanActivateFn, Router} from '@angular/router';
import {inject} from "@angular/core";
import {AuthService} from "../services/auth.service";
import {catchError, map, of} from "rxjs";
import {WorkerAccountService} from "../services/worker-account.service";
import {HttpErrorResponse} from "@angular/common/http";
import {UserRole} from "../models/roles";

export const workerOnlyGuard: CanActivateFn = (route, state) => {
  const accountService = inject(WorkerAccountService);
  const authService = inject(AuthService);
  const router = inject(Router);

  return accountService.getCurrentAccount().pipe(
    map(user => {
      if (user?.role === UserRole.WORKER) {
        return true;
      }

      router.navigate(['/account']);
      return false;
    }),
    catchError((error) => {
      if (error instanceof HttpErrorResponse) {

        // 401 (not logged in) or 403 (forbidden / session expired)
        if (error.status === 401 || error.status === 403) {
          console.warn(`Accès refusé : utilisateur non connecté ou session expirée (Code ${error.status})`);
          authService.setRedirectUrl(state.url);
          router.navigate(['/login']);
          return of(false);
        }

        // 500 (server error) or other codes (e.g. 503, 0 for network)
        console.error(`Erreur technique serveur (Code ${error.status}) lors de la validation du profil :`, error.message);
        // Not a login problem: send to the default space instead of the login page
        router.navigate(['/account']);
        return of(false);
      }

      // Fallback when the error is not an HTTP response (e.g. a JS error)
      console.error("Une erreur inattendue est survenue dans le guard :", error);
      router.navigate(['/account']);
      return of(false);
    })
  );
};
