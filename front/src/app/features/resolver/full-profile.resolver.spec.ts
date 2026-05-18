import { TestBed } from '@angular/core/testing';
import { ResolveFn } from '@angular/router';

import { fullProfileResolver } from './full-profile.resolver';

describe('fullProfileResolver', () => {
  const executeResolver: ResolveFn<boolean> = (...resolverParameters) => 
      TestBed.runInInjectionContext(() => fullProfileResolver(...resolverParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeResolver).toBeTruthy();
  });
});
