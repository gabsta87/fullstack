import {ResolveFn} from '@angular/router';
import {WorkerService} from '../services/worker.service';
import {inject} from "@angular/core";
import {Service} from "../models/common.model";

export const servicesResolver: ResolveFn<Service[]> = () => {
  return inject(WorkerService).getWorkersServices();
};
