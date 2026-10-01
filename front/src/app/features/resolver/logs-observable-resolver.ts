import { ResolveFn } from '@angular/router';
import {Observable} from "rxjs";
import {AdminLog} from "../models/common.model";
import {inject} from "@angular/core";
import {AdminService} from "../services/admin-service";

export const logsObservableResolver: ResolveFn<Observable<AdminLog[]>> = (route, state) => {
  return inject(AdminService).getAuditLogs();
};
