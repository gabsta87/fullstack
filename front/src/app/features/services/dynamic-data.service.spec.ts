import { TestBed } from '@angular/core/testing';

import { DynamicDataService } from './dynamic-data.service';

describe('DynamicDataService', () => {
  let service: DynamicDataService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DynamicDataService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
