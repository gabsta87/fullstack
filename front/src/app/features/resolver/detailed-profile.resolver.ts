import { ResolveFn } from '@angular/router';
import {ProfileDetail} from "../components/profile/profile.component";
import {inject} from "@angular/core";
import {DynamicDataService} from "../services/dynamic-data.service";
import {Observable} from "rxjs";

export const detailedProfileResolver: ResolveFn<Observable<ProfileDetail>> = (route, state) => {
  const dataService = inject(DynamicDataService);

  const profileId = route.queryParamMap.get('id');

  if (!profileId || isNaN(Number(profileId))) {
    throw new Error("Profile ID is missing or not a valid number.");
  }
  return dataService.getTableDataById("worker", parseInt(profileId, 10));
};
