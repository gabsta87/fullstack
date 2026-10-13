import {ResolveFn, Router} from '@angular/router';
import {inject} from "@angular/core";
import {WorkerService} from "../services/worker.service";
import {catchError, map, of} from "rxjs";
import { WorkerPublicFullProfile } from "../models/user.model";
import {AuthService} from "../services/auth.service";

export const profileVisitingResolver: ResolveFn<any> = (route, state) => {
  const workerService = inject(WorkerService);
  const authService = inject(AuthService);
  const router = inject(Router);
  const workerId = route.queryParamMap.get('id');

  if (!workerId) {
    router.navigate(['/']);
    return of(null);
  }

  return workerService.getProfile(workerId).pipe(
    map(profile => {
      // 1. Vérifier si l'utilisateur connecté est un Admin
      const tokenData = authService.getDecodedToken();
      const role = tokenData?.role;
      const isAdmin = role === 'ROLE_ADMIN' || role === 'ROLE_SUPER_ADMIN';

      // 2. Si le profil est banni/verrouillé et que ce n'est PAS un admin -> Redirection
      if (!isAdmin && (profile.banned || profile.locked)) {
        router.navigate(['/']);
        return null; // Bloque le rendu de la page
      }

      // 3. Sinon, on laisse passer et on transmet le profil au composant
      return profile;
    }),
    catchError(() => {
      // En cas d'erreur (profil introuvable ou 403 du back), on redirige vers l'accueil
      router.navigate(['/']);
      return of(null);
    })
  );
};
