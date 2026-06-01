import {CanActivateFn, Router} from '@angular/router';
import {inject} from "@angular/core";
import {AuthService} from "../services/auth.service";
import {catchError, map, of} from "rxjs";
import {ClientAccountService} from "../services/client-account.service";
import {WorkerAccountService} from "../services/worker-account.service";

export const workerOnlyGuard: CanActivateFn = (route, state) => {
  const accountService = inject(WorkerAccountService);
  const authService = inject(AuthService); // Injection nécessaire pour redirectUrl
  const router = inject(Router);

  return accountService.getCurrentAccount().pipe(
    map(user => {
      // SCÉNARIO 1 : Utilisateur authentifié
      if (user?.role === 'WORKER') {
        return true; // Accès autorisé
      }

      // SCÉNARIO 2 : Authentifié mais pas WORKER (ex: CLIENT)
      console.log("Accès refusé : utilisateur n'est pas un worker");
      router.navigate(['/account']); // Ou la route par défaut pour les non-workers
      return false;
    }),
    catchError(() => {
      // SCÉNARIO 3 : Pas authentifié (L'appel API a échoué, généralement avec une 401)
      console.log("Accès refusé : utilisateur non connecté");
      authService.setRedirectUrl(state.url); // On garde l'URL en mémoire pour le retour
      router.navigate(['/login']);
      return of(false);
    })
  );
};
