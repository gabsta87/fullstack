import { ResolveFn } from '@angular/router';
import {Observable} from "rxjs";
import {CertificationRequest} from "../models/common.model";
import {inject} from "@angular/core";
import {AdminService} from "../services/admin-service";

export const certificationsObservableResolver: ResolveFn<Observable<CertificationRequest[]>> = (route, state) => {
  return inject(AdminService).getCertificationRequests();
};
