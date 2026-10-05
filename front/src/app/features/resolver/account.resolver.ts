import {ResolveFn} from '@angular/router';
import {inject} from "@angular/core";
import {ClientAccountService} from "../services/client-account.service";
import {ClientProfile} from "../models/user.model";

export const accountResolver: ResolveFn<ClientProfile> = (route, state) => {
  const accountService = inject(ClientAccountService);

  return accountService.getCurrentAccount();
};
