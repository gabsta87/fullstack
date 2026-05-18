import { TestBed } from '@angular/core/testing';

import { LanguageManagerService } from './language-manager.service';

describe('LanguageManagerService', () => {
  let service: LanguageManagerService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(LanguageManagerService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
