import {ResolveFn} from '@angular/router';
import {inject} from "@angular/core";
import {ClientAccountService} from "../services/client-account.service";
import {ClientPrivateAccount} from "../models/user.model";

export const accountResolver: ResolveFn<ClientPrivateAccount> = (route, state) => {
  const accountService = inject(ClientAccountService);

  return accountService.getCurrentAccount();
};
