import {ResolveFn} from '@angular/router';
import {inject} from "@angular/core";
import {WorkerService} from "../services/worker.service";
import {catchError, of} from "rxjs";
import {WorkerFullProfile} from "../models/user.model";

export const profileVisitingResolver: ResolveFn<WorkerFullProfile | null> = (route) => {
  const workerService = inject(WorkerService);
  const id = route.queryParamMap.get('id');

  if (!id) return of(null);

  const cached = workerService.getCachedProfile(id);
  if (cached) return of(cached);

  return workerService.getProfile(id).pipe(
    catchError((err) => {
      console.error("Erreur lors de la récupération du profil :", err);
      return of(null);
    })
  );
};
