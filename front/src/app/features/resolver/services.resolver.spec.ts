import { TestBed } from '@angular/core/testing';
import { ResolveFn } from '@angular/router';

import { servicesResolver } from './services.resolver';
import {Service} from "../models/common.model";

describe('servicesResolver', () => {
  const executeResolver: ResolveFn<Service[]> = (...resolverParameters) =>
      TestBed.runInInjectionContext(() => servicesResolver(...resolverParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeResolver).toBeTruthy();
  });
});
