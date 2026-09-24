import { TestBed } from '@angular/core/testing';
import { ResolveFn } from '@angular/router';

import { usersObservableResolver } from './users-observable-resolver';
import {Observable} from "rxjs";

describe('usersObservableResolver', () => {
  const executeResolver: ResolveFn<Observable<any[]>> = (...resolverParameters) =>
      TestBed.runInInjectionContext(() => usersObservableResolver(...resolverParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeResolver).toBeTruthy();
  });
});
