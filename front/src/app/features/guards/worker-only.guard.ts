import {CanActivateFn, Router} from '@angular/router';
import {inject} from "@angular/core";
import {AuthService} from "../services/auth.service";
import {catchError, map, of} from "rxjs";
import {WorkerAccountService} from "../services/worker-account.service";
import {HttpErrorResponse} from "@angular/common/http";

export const workerOnlyGuard: CanActivateFn = (route, state) => {
  const accountService = inject(WorkerAccountService);
  const authService = inject(AuthService);
  const router = inject(Router);

  return accountService.getCurrentAccount().pipe(
    map(user => {
      if (user?.role === 'WORKER') {
        return true;
      }

      console.log("Accès refusé : utilisateur n'est pas un worker");
      router.navigate(['/account']);
      return false;
    }),
    catchError((error) => {
      if (error instanceof HttpErrorResponse) {

        // Cas 401 (Non connecté) ou 403 (Interdit / Session expirée)
        if (error.status === 401 || error.status === 403) {
          console.warn(`Accès refusé : utilisateur non connecté ou session expirée (Code ${error.status})`);
          authService.setRedirectUrl(state.url);
          router.navigate(['/login']);
          return of(false);
        }

        // Cas 500 (Erreur Serveur) ou autres codes d'erreurs (ex: 503, 0 pour réseau)
        console.error(`Erreur technique serveur (Code ${error.status}) lors de la validation du profil :`, error.message);
        // Au lieu de renvoyer sur le login (ce qui est faux), on redirige vers l'espace par défaut ou une page d'erreur
        router.navigate(['/account']);
        return of(false);
      }

      // Fallback si l'erreur n'est pas une réponse HTTP (ex: erreur de code JS)
      console.error("Une erreur inattendue est survenue dans le guard :", error);
      router.navigate(['/account']);
      return of(false);
    })
  );
};
