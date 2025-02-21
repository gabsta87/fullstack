import { TestBed } from '@angular/core/testing';
import { CanActivateFn } from '@angular/router';

import { loggedAsAdminGuard } from './logged-as-admin.guard';

describe('loggedAsAdminGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) => 
      TestBed.runInInjectionContext(() => loggedAsAdminGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
