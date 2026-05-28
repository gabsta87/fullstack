import { ResolveFn } from '@angular/router';
import { inject } from "@angular/core";
import { DynamicDataService } from "../services/dynamic-data.service";
import {WorkerSimpleProfile} from "../models/user.model";

export const simpleProfilesResolver: ResolveFn<WorkerSimpleProfile[]> = (route, state) => {
  const dataService = inject(DynamicDataService);
  return dataService.getSimpleProfiles();
};
