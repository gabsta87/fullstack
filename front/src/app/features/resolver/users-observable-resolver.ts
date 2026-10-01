import { ResolveFn } from '@angular/router';
import {Observable} from "rxjs";
import {AdminService} from "../services/admin-service";
import {inject} from "@angular/core";
import {WorkerProfileForAdmin} from "../models/user.model";

export const usersObservableResolver: ResolveFn<Observable<WorkerProfileForAdmin[]>> = () => {
  return inject(AdminService).getUsers();
};
