import { TestBed } from '@angular/core/testing';
import { ResolveFn } from '@angular/router';

import { geographicZonesResolver } from './geographic-zones.resolver';

describe('geographicZonesResolver', () => {
  const executeResolver: ResolveFn<boolean> = (...resolverParameters) => 
      TestBed.runInInjectionContext(() => geographicZonesResolver(...resolverParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeResolver).toBeTruthy();
  });
});
