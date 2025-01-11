import { TestBed } from '@angular/core/testing';
import { ResolveFn } from '@angular/router';

import { simpleProfilesResolver } from './simple-profiles.resolver';

describe('simpleProfilesResolver', () => {
  const executeResolver: ResolveFn<boolean> = (...resolverParameters) => 
      TestBed.runInInjectionContext(() => simpleProfilesResolver(...resolverParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeResolver).toBeTruthy();
  });
});
