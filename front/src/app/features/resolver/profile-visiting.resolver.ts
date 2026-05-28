import {ResolveFn, Router} from '@angular/router';
import {inject} from "@angular/core";
import {WorkerService} from "../services/worker.service";
import {catchError, EMPTY, of} from "rxjs";
import {WorkerFullProfile} from "../models/user.model";

export const profileVisitingResolver: ResolveFn<WorkerFullProfile> = (route, state) => {
  const workerService = inject(WorkerService);
  const router        = inject(Router);

  const id = route.queryParamMap.get('id');

  if (!id) {
    router.navigate(['/']);
    return EMPTY;
  }

  // Return from prefetch cache immediately if available
  const cached = workerService.getCachedProfile(id);
  if (cached) return of(cached);

  return workerService.getProfile(id).pipe(
    catchError(() => {
      router.navigate(['/']);
      return EMPTY;
    })
  );
};
