import { TestBed } from '@angular/core/testing';
import { CanActivateFn } from '@angular/router';

import { workerOnlyGuard } from './worker-only.guard';

describe('workerOnlyGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) => 
      TestBed.runInInjectionContext(() => workerOnlyGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
