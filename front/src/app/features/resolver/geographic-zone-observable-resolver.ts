import {ResolveFn} from '@angular/router';
import {inject} from "@angular/core";
import {CommonService} from "../services/common-service";
import {Observable} from "rxjs";
import {GeographicZoneWithParent} from "../models/filter.model";

export const geographicZoneObservableResolver: ResolveFn<Observable<GeographicZoneWithParent[]>> = () => {
  return inject(CommonService).getGeographicZones();
};
