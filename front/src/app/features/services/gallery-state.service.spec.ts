import { TestBed } from '@angular/core/testing';

import { GalleryStateService } from './gallery-state.service';

describe('GalleryStateService', () => {
  let service: GalleryStateService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(GalleryStateService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
