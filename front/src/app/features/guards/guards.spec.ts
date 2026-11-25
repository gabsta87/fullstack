import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom, isObservable, of, throwError, BehaviorSubject } from 'rxjs';
import { authGuard } from './auth.guard';
import { adminOnlyGuard } from './admin-only.guard';
import { workerOnlyGuard } from './worker-only.guard';
import { accountRedirectGuard } from './account-redirect-guard';
import { AuthService } from '../services/auth.service';
import { WorkerAccountService } from '../services/worker-account.service';
import { UserRole } from '../models/roles';

describe('Route guards', () => {
  let authService: jasmine.SpyObj<AuthService> & { isAuthenticated: boolean; isAdmin: boolean };
  let workerService: jasmine.SpyObj<WorkerAccountService>;
  let router: Router;
  const state = { url: '/profile-management' } as RouterStateSnapshot;
  const route = {} as ActivatedRouteSnapshot;

  async function run(guard: any): Promise<boolean | UrlTree> {
    const result = TestBed.runInInjectionContext(() => guard(route, state));
    const awaited = result instanceof Promise ? await result : result;
    return (isObservable(awaited) ? await firstValueFrom(awaited) : awaited) as boolean | UrlTree;
  }

  const urlOf = (r: boolean | UrlTree) => (r instanceof UrlTree ? router.serializeUrl(r) : r);

  beforeEach(() => {
    authService = jasmine.createSpyObj('AuthService', ['checkSession', 'setRedirectUrl', 'getUser'], {
      isAuthenticated$: new BehaviorSubject(false),
    }) as any;
    (authService as any).isAuthenticated = false;
    (authService as any).isAdmin = false;
    workerService = jasmine.createSpyObj('WorkerAccountService', ['getCurrentAccount']);

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: WorkerAccountService, useValue: workerService },
      ],
    });
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    // The guards report refusals with console.warn / console.error: keep the test output clean.
    spyOn(console, 'warn');
    spyOn(console, 'error');
  });

  describe('authGuard', () => {
    it('lets a known session through without a server call', async () => {
      authService.isAuthenticated = true;
      expect(await run(authGuard)).toBeTrue();
      expect(authService.checkSession).not.toHaveBeenCalled();
    });

    it('lets through when the server confirms the session', async () => {
      authService.checkSession.and.returnValue(of(true));
      expect(await run(authGuard)).toBeTrue();
    });

    it('blocks and remembers the URL when the server has no session', async () => {
      authService.checkSession.and.returnValue(of(false));
      expect(urlOf(await run(authGuard))).toBe('/');
      expect(authService.setRedirectUrl).toHaveBeenCalledWith('/profile-management');
    });

    it('blocks on a network error', async () => {
      authService.checkSession.and.returnValue(throwError(() => new Error('offline')));
      expect(urlOf(await run(authGuard))).toBe('/');
    });
  });

  describe('adminOnlyGuard', () => {
    it('lets admins through', async () => {
      authService.isAdmin = true;
      expect(await run(adminOnlyGuard)).toBeTrue();
    });

    it('redirects non-admins home after checking the session', async () => {
      authService.checkSession.and.returnValue(of(true));
      expect(urlOf(await run(adminOnlyGuard))).toBe('/');
    });

    it('redirects home on error', async () => {
      authService.checkSession.and.returnValue(throwError(() => new Error('x')));
      expect(urlOf(await run(adminOnlyGuard))).toBe('/');
    });
  });

  describe('workerOnlyGuard', () => {
    it('lets workers through', async () => {
      workerService.getCurrentAccount.and.returnValue(of({ role: UserRole.WORKER } as any));
      expect(await run(workerOnlyGuard)).toBeTrue();
    });

    it('refuses clients and admins', async () => {
      for (const role of [UserRole.CLIENT, UserRole.ADMIN, UserRole.SUPER_ADMIN]) {
        workerService.getCurrentAccount.and.returnValue(of({ role } as any));
        expect(await run(workerOnlyGuard)).withContext(role).toBeFalse();
      }
    });

    it('refuses and remembers the URL on 401 / 403', async () => {
      for (const status of [401, 403]) {
        workerService.getCurrentAccount.and.returnValue(throwError(() => new HttpErrorResponse({ status })));
        expect(await run(workerOnlyGuard)).toBeFalse();
      }
      expect(authService.setRedirectUrl).toHaveBeenCalledWith('/profile-management');
    });

    it('refuses on server errors', async () => {
      workerService.getCurrentAccount.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
      expect(await run(workerOnlyGuard)).toBeFalse();
    });
  });

  describe('accountRedirectGuard', () => {
    const cases: [string, string][] = [
      [UserRole.ADMIN, '/admin-dashboard'],
      [UserRole.SUPER_ADMIN, '/admin-dashboard'],
      [UserRole.WORKER, '/profile-management'],
      [UserRole.CLIENT, '/account'],
      ['SOMETHING_ELSE', '/'],
    ];

    for (const [role, target] of cases) {
      it(`sends ${role} to ${target}`, async () => {
        (authService.isAuthenticated$ as BehaviorSubject<boolean>).next(true);
        authService.getUser.and.returnValue({ role } as any);
        expect(urlOf(await run(accountRedirectGuard))).toBe(target);
      });
    }

    it('sends anonymous visitors home', async () => {
      expect(urlOf(await run(accountRedirectGuard))).toBe('/');
    });
  });
});
