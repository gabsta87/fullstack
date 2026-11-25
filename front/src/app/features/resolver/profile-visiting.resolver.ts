import {RedirectCommand, ResolveFn, Router} from '@angular/router';
import {inject} from "@angular/core";
import {WorkerService} from "../services/worker.service";
import {catchError, of} from "rxjs";
import { WorkerPublicFullProfile } from "../models/user.model";

/**
 * Loads the visited profile. A profile that is not public (banned, hidden, incomplete) answers 404,
 * except to admins and to its owner: the visitor is then sent back to the gallery.
 */
export const profileVisitingResolver: ResolveFn<WorkerPublicFullProfile | null> = (route) => {
  const workerService = inject(WorkerService);
  const router = inject(Router);
  const home = new RedirectCommand(router.parseUrl('/'));
  const id = route.queryParamMap.get('id');

  if (!id) return home;

  const cached = workerService.getCachedProfile(id);
  if (cached) return of(cached);

  return workerService.getProfile(id).pipe(
    catchError((err) => {
      if (err?.status === 404) return of(home);
      console.error('Could not load the profile', err);
      return of(null);
    })
  );
};
