import { ResolveFn } from '@angular/router';
import {Profile} from "../components/profile/profile.component";
import {inject} from "@angular/core";
import {DynamicDataService} from "../services/dynamic-data.service";

export const simpleProfilesResolver: ResolveFn<Profile[]> = (route, state) => {
  const dataService = inject(DynamicDataService);
  return dataService.getSimpleProfiles();
};
