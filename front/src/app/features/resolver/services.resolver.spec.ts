import { TestBed } from '@angular/core/testing';
import { ResolveFn } from '@angular/router';

import { servicesResolver } from './services.resolver';

describe('servicesResolver', () => {
  const executeResolver: ResolveFn<boolean> = (...resolverParameters) => 
      TestBed.runInInjectionContext(() => servicesResolver(...resolverParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeResolver).toBeTruthy();
  });
});
