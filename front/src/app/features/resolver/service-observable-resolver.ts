import {ResolveFn} from '@angular/router';
import {inject} from "@angular/core";
import {Observable} from "rxjs";
import {Service} from "../models/common.model";
import {CommonService} from "../services/common-service";

export const serviceObservableResolver: ResolveFn<Observable<Service[]>> = () => {
  return inject(CommonService).getWorkersServices();
};
