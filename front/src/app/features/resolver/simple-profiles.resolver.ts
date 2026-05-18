import { ResolveFn } from '@angular/router';
import { WorkerSimpleProfile } from "../models/worker.model";
import { inject } from "@angular/core";
import { DynamicDataService } from "../services/dynamic-data.service";

export const simpleProfilesResolver: ResolveFn<WorkerSimpleProfile[]> = (route, state) => {
  const dataService = inject(DynamicDataService);
  return dataService.getSimpleProfiles();
};
