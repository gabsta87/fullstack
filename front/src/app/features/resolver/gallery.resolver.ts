import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { WorkerService } from '../services/worker.service';
import { WorkerGalleryDTO } from '../models/worker.model';
import { catchError, of } from 'rxjs';

/**
 * Loads the first page of gallery cards before the homepage renders.
 * This gives us the mainThumbUrl for each worker immediately —
 * no empty-card flash on first load.
 *
 * Subsequent pages (infinite scroll) are loaded directly by the
 * HomepageComponent via WorkerService, not through the resolver.
 */
export const galleryResolver: ResolveFn<WorkerGalleryDTO[]> = () => {
  const workerService = inject(WorkerService);
  return workerService.getGalleryPage(0, {}).pipe(
    catchError(() => of([]))
  );
};
