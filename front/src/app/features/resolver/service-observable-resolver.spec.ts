import { TestBed } from '@angular/core/testing';
import { ResolveFn } from '@angular/router';

import { serviceObservableResolver } from './service-observable-resolver';
import {Observable} from "rxjs";
import {Service} from "../models/common.model";

describe('serviceObservableResolver', () => {
  const executeResolver: ResolveFn<Observable<Service[]>> = (...resolverParameters) =>
      TestBed.runInInjectionContext(() => serviceObservableResolver(...resolverParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeResolver).toBeTruthy();
  });
});
