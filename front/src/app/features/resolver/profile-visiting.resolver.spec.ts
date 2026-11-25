import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, convertToParamMap, provideRouter, RedirectCommand, RouterStateSnapshot } from '@angular/router';
import { firstValueFrom, isObservable, of, throwError } from 'rxjs';
import { profileVisitingResolver } from './profile-visiting.resolver';
import { WorkerService } from '../services/worker.service';

describe('profileVisitingResolver', () => {
  let workerService: jasmine.SpyObj<WorkerService>;

  async function resolve(id: string | null) {
    const route = { queryParamMap: convertToParamMap(id ? { id } : {}) } as ActivatedRouteSnapshot;
    const result: any = TestBed.runInInjectionContext(() => profileVisitingResolver(route, {} as RouterStateSnapshot));
    return isObservable(result) ? firstValueFrom(result) : result;
  }

  beforeEach(() => {
    workerService = jasmine.createSpyObj('WorkerService', ['getCachedProfile', 'getProfile']);
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: WorkerService, useValue: workerService }] });
  });

  const isRedirectHome = (value: any) =>
    value instanceof RedirectCommand && value.redirectTo.toString() === '/';

  it('sends the visitor home without an id', async () => {
    expect(isRedirectHome(await resolve(null))).toBeTrue();
    expect(workerService.getProfile).not.toHaveBeenCalled();
  });

  it('uses the cache when possible', async () => {
    workerService.getCachedProfile.and.returnValue({ id: 'x' } as any);
    expect(await resolve('x')).toEqual({ id: 'x' } as any);
    expect(workerService.getProfile).not.toHaveBeenCalled();
  });

  it('sends the visitor home when the profile is not public (banned, hidden: 404)', async () => {
    workerService.getCachedProfile.and.returnValue(undefined as any);
    workerService.getProfile.and.returnValue(throwError(() => ({ status: 404 })));
    expect(isRedirectHome(await resolve('banned-id'))).toBeTrue();
  });

  it('resolves null on a server error instead of breaking navigation', async () => {
    workerService.getCachedProfile.and.returnValue(undefined as any);
    workerService.getProfile.and.returnValue(throwError(() => ({ status: 500 })));
    expect(await resolve('any-id')).toBeNull();
  });

  it('fetches the profile otherwise', async () => {
    workerService.getCachedProfile.and.returnValue(undefined as any);
    workerService.getProfile.and.returnValue(of({ id: 'y' } as any));
    expect(await resolve('y')).toEqual({ id: 'y' } as any);
  });
});
