import { CanActivateFn } from '@angular/router';
import {inject} from "@angular/core";
import {ClientAccountService} from "../services/client-account.service";
import {map} from "rxjs";

export const adminOnlyGuard: CanActivateFn = (route, state) => {
  const accountService = inject(ClientAccountService);

  return accountService.getCurrentAccount().pipe(
    map(user => {
      if (user?.role === 'ADMIN') return true;
      return false;
    })
  );
};
