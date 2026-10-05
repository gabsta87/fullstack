import { ResolveFn } from '@angular/router';
import { inject } from "@angular/core";
import { DynamicDataService } from "../services/dynamic-data.service";
import { WorkerMinimalProfile } from "../models/user.model";

export const simpleProfilesResolver: ResolveFn<WorkerMinimalProfile[]> = (route, state) => {
  return inject(DynamicDataService).getSimpleProfiles();
};
