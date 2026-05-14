import { ResolveFn } from '@angular/router';
import {firstValueFrom} from "rxjs";
import { WorkerService } from '../services/worker.service';
import {inject} from "@angular/core";

export const servicesResolver: ResolveFn<string[]> = (route, state) => {
  const workerService = inject(WorkerService);
  return firstValueFrom(workerService.getWorkersServices());
};
