import {ResolveFn} from '@angular/router';
import {inject} from "@angular/core";
import {Observable} from "rxjs";
import {WorkerPrivateProfile} from "../models/user.model";
import {WorkerAccountService} from "../services/worker-account.service";

export const profileManagementResolver: ResolveFn<Observable<WorkerPrivateProfile>> = (route, state) => {
  return inject(WorkerAccountService).getCurrentAccount();
};
