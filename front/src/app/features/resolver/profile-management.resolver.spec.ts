import { TestBed } from '@angular/core/testing';
import { ResolveFn } from '@angular/router';

import { profileManagementResolver } from './profile-management.resolver';

describe('profileManagementResolver', () => {
  const executeResolver: ResolveFn<boolean> = (...resolverParameters) => 
      TestBed.runInInjectionContext(() => profileManagementResolver(...resolverParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeResolver).toBeTruthy();
  });
});
