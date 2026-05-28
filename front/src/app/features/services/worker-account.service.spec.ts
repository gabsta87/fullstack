import { TestBed } from '@angular/core/testing';

import { WorkerAccountService } from './worker-account.service';

describe('WorkerAccountService', () => {
  let service: WorkerAccountService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(WorkerAccountService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
