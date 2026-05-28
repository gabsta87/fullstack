import {ResolveFn, Router} from '@angular/router';
import {inject} from "@angular/core";
import {EMPTY, map, Observable} from "rxjs";
import {WorkerPrivateAccount} from "../models/user.model";
import {WorkerAccountService} from "../services/worker-account.service";

export const profileManagementResolver: ResolveFn<Observable<WorkerPrivateAccount>> = (route, state) => {
  const userService = inject(WorkerAccountService);
  const router = inject(Router);

  const user = userService.getCurrentAccount();

  if (user?.pipe(map(e => {e.role === 'CLIENT'}))) {
    router.navigate(['/account']);
    return EMPTY;
  }

  return user;
};
