import { ResolveFn } from '@angular/router';
import {GeographicZone} from "../models/filter.model";
import {inject} from "@angular/core";
import {WorkerService} from "../services/worker.service";
import {CommonService} from "../services/common-service";

export const geographicZonesResolver: ResolveFn<GeographicZone[]> = () => {
  return inject(CommonService).getGeographicZones();
};
