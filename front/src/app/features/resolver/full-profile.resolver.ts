import { ResolveFn } from '@angular/router';
import {inject} from "@angular/core";
import {DynamicDataService} from "../services/dynamic-data.service";
import {Observable} from "rxjs";
import {WorkerProfile} from "../models/worker.model";

export const fullProfileResolver: ResolveFn<Observable<WorkerProfile>> = (route, state) => {
  const dataService = inject(DynamicDataService);
  return dataService.getFullProfile(route.params['id']);
};
