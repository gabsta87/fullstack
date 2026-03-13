import { inject } from '@angular/core';
import { ResolveFn, Router } from '@angular/router';
import { WorkerService } from '../services/worker.service';
import { WorkerProfile } from '../models/worker.model';
import { catchError, EMPTY } from 'rxjs';

/**
 * Loads the full WorkerProfile before the profile page renders.
 *
 * If the profile was prefetched during hover (stored in WorkerService cache),
 * it returns instantly from cache. Otherwise it fetches from the API.
 *
 * Reads ?id= as a UUID string — matches the Worker entity's UUID primary key.
 */
export const detailedProfileResolver: ResolveFn<WorkerProfile> = (route) => {
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

// needed for of()
import { of } from 'rxjs';
