import { TestBed } from '@angular/core/testing';
import { CanActivateFn } from '@angular/router';

import { accountRedirectGuard } from './account-redirect-guard';

describe('accountRedirectGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) => 
      TestBed.runInInjectionContext(() => accountRedirectGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
