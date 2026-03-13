import { TestBed } from '@angular/core/testing';
import { ResolveFn } from '@angular/router';

import { galleryResolver } from './gallery.resolver';

describe('galleryResolver', () => {
  const executeResolver: ResolveFn<boolean> = (...resolverParameters) => 
      TestBed.runInInjectionContext(() => galleryResolver(...resolverParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeResolver).toBeTruthy();
  });
});
