import { CanActivateFn } from '@angular/router';
import {inject} from "@angular/core";
import {AuthService} from "../services/auth.service";
import {map} from "rxjs";
import {ClientAccountService} from "../services/client-account.service";

export const workerOnlyGuard: CanActivateFn = (route, state) => {
  const accountService = inject(ClientAccountService);

  return accountService.getCurrentAccount().pipe(
    map(user => {
      if (user?.role === 'WORKER') return true;
      return false;
    })
  );
};
