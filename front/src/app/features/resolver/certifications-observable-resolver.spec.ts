import { TestBed } from '@angular/core/testing';
import { ResolveFn } from '@angular/router';

import { certificationsObservableResolver } from './certifications-observable-resolver';

describe('certificationsObservableResolver', () => {
  const executeResolver: ResolveFn<boolean> = (...resolverParameters) => 
      TestBed.runInInjectionContext(() => certificationsObservableResolver(...resolverParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeResolver).toBeTruthy();
  });
});
