import {ResolveFn} from '@angular/router';
import {inject} from "@angular/core";
import {Observable} from "rxjs";
import {WorkerPrivateAccount} from "../models/user.model";
import {WorkerAccountService} from "../services/worker-account.service";

export const profileManagementResolver: ResolveFn<Observable<WorkerPrivateAccount>> = (route, state) => {
  const userService = inject(WorkerAccountService);

  return userService.getCurrentAccount();
};
