import { TestBed } from '@angular/core/testing';
import { ResolveFn } from '@angular/router';

import { detailedProfileResolver } from './detailed-profile.resolver';

describe('detailedProfileResolver', () => {
  const executeResolver: ResolveFn<boolean> = (...resolverParameters) => 
      TestBed.runInInjectionContext(() => detailedProfileResolver(...resolverParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeResolver).toBeTruthy();
  });
});
