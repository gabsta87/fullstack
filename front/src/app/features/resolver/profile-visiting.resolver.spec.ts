import { TestBed } from '@angular/core/testing';
import { ResolveFn } from '@angular/router';

import { profileVisitingResolver } from './profile-visiting.resolver';

describe('profileVisitingResolver', () => {
  const executeResolver: ResolveFn<boolean> = (...resolverParameters) => 
      TestBed.runInInjectionContext(() => profileVisitingResolver(...resolverParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeResolver).toBeTruthy();
  });
});
