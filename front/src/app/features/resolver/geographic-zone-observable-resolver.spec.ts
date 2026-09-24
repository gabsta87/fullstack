import { TestBed } from '@angular/core/testing';
import { ResolveFn } from '@angular/router';

import { geographicZoneObservableResolver } from './geographic-zone-observable-resolver';

describe('geographicZoneObservableResolver', () => {
  const executeResolver: ResolveFn<boolean> = (...resolverParameters) => 
      TestBed.runInInjectionContext(() => geographicZoneObservableResolver(...resolverParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeResolver).toBeTruthy();
  });
});
