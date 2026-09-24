import { ResolveFn } from '@angular/router';
import {Observable} from "rxjs";
import {AdminService} from "../services/admin-service";
import {inject} from "@angular/core";

export const usersObservableResolver: ResolveFn<Observable<any[]>> = () => {
  return inject(AdminService).getUsers();
};
