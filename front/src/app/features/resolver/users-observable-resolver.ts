import { ResolveFn } from '@angular/router';
import {Observable} from "rxjs";
import {AdminService} from "../services/admin-service";
import {inject} from "@angular/core";
import {AdminUser} from "../models/user.model";

export const usersObservableResolver: ResolveFn<Observable<AdminUser[]>> = () => {
  return inject(AdminService).getUsers();
};
