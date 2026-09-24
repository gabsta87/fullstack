import { ResolveFn } from '@angular/router';
import {inject} from "@angular/core";
import {CommonService} from "../services/common-service";
import {Observable} from "rxjs";
import {GeographicZone} from "../models/filter.model";

export const geographicZoneObservableResolver: ResolveFn<Observable<GeographicZone[]>> = () => {
  return inject(CommonService).getGeographicZones();
};
