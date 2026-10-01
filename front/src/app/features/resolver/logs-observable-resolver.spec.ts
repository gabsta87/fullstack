import { TestBed } from '@angular/core/testing';
import { ResolveFn } from '@angular/router';

import { logsObservableResolver } from './logs-observable-resolver';

describe('logsObservableResolver', () => {
  const executeResolver: ResolveFn<boolean> = (...resolverParameters) => 
      TestBed.runInInjectionContext(() => logsObservableResolver(...resolverParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeResolver).toBeTruthy();
  });
});
