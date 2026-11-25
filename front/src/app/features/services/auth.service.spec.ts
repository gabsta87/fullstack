import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { WorkerAccountService } from './worker-account.service';
import { ClientAccountService } from './client-account.service';
import { environment } from '../../../environments/environment';
import { FakeEventSource } from '../../../testing/fake-event-source';
import { UserRole } from '../models/roles';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let router: jasmine.SpyObj<Router>;
  let workerService: jasmine.SpyObj<WorkerAccountService>;
  let clientService: jasmine.SpyObj<ClientAccountService>;
  let restoreEventSource: () => void;
  const auth = `${environment.apiBase}/auth`;

  beforeEach(() => {
    localStorage.clear();
    restoreEventSource = FakeEventSource.install();
    router = jasmine.createSpyObj('Router', ['navigate']);
    workerService = jasmine.createSpyObj('WorkerAccountService', ['clearCache', 'updateCache']);
    clientService = jasmine.createSpyObj('ClientAccountService', ['clearCache', 'updateCache']);

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
        { provide: WorkerAccountService, useValue: workerService },
        { provide: ClientAccountService, useValue: clientService },
      ],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    restoreEventSource();
    localStorage.clear();
  });

  function login(role: string) {
    service.login('a@venus.test', 'Correct-Horse-42').subscribe();
    httpMock.expectOne(`${auth}/login`).flush({ token: 'jwt-token', user: { id: '1', username: 'a', role } });
  }

  it('login stores the token and the user', () => {
    login(UserRole.CLIENT);
    expect(localStorage.getItem('auth_token')).toBe('jwt-token');
    expect(service.isAuthenticated).toBeTrue();
    expect(service.getUser()?.role).toBe(UserRole.CLIENT);
  });

  it('a failed login stores nothing', () => {
    let error: any;
    service.login('a@venus.test', 'bad').subscribe({ error: e => (error = e) });
    httpMock.expectOne(`${auth}/login`).flush({ error: 'Invalid credentials.' }, { status: 401, statusText: 'Unauthorized' });
    expect(error.status).toBe(401);
    expect(localStorage.getItem('auth_token')).toBeNull();
    expect(service.isAuthenticated).toBeFalse();
  });

  it('isAdmin is true for ADMIN and SUPER_ADMIN only', () => {
    for (const [role, expected] of [[UserRole.ADMIN, true], [UserRole.SUPER_ADMIN, true], [UserRole.WORKER, false], [UserRole.CLIENT, false]] as const) {
      login(role);
      expect(service.isAdmin).withContext(role).toBe(expected);
    }
  });

  it('checkSession returns true for a valid session', () => {
    let result: boolean | undefined;
    service.checkSession().subscribe(r => (result = r));
    httpMock.expectOne(`${auth}/session-check`).flush({ id: '1', username: 'a', role: UserRole.WORKER });
    expect(result).toBeTrue();
    expect(service.isAuthenticated).toBeTrue();
  });

  it('checkSession logs out locally when the server rejects the token (expired, revoked, banned)', () => {
    localStorage.setItem('auth_token', 'revoked');
    let result: boolean | undefined;
    service.checkSession().subscribe(r => (result = r));
    httpMock.expectOne(`${auth}/session-check`).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(result).toBeFalse();
    expect(localStorage.getItem('auth_token')).toBeNull();
    expect(workerService.clearCache).toHaveBeenCalled();
    expect(clientService.clearCache).toHaveBeenCalled();
  });

  it('checkSession is cached for a minute', () => {
    service.checkSession().subscribe();
    httpMock.expectOne(`${auth}/session-check`).flush({ id: '1', role: UserRole.CLIENT });
    let second: boolean | undefined;
    service.checkSession().subscribe(r => (second = r));
    httpMock.expectNone(`${auth}/session-check`);
    expect(second).toBeTrue();
  });

  it('logout clears everything even if the server call fails', () => {
    login(UserRole.CLIENT);
    service.logout().subscribe();
    httpMock.expectOne(`${auth}/logout`).flush({}, { status: 500, statusText: 'Error' });
    expect(localStorage.getItem('auth_token')).toBeNull();
    expect(service.isAuthenticated).toBeFalse();
    expect(service.getUser()).toBeNull();
  });

  it('opens the event stream after login and closes it on logout', fakeAsync(() => {
    login(UserRole.WORKER);
    tick(250);
    expect(FakeEventSource.instances.length).toBe(1);
    expect(FakeEventSource.instances[0].url).toContain('/account/stream?token=jwt-token');

    service.logout().subscribe();
    httpMock.expectOne(`${auth}/logout`).flush({});
    expect(FakeEventSource.instances[0].closed).toBeTrue();
  }));

  it('live account updates reach the worker cache (certification updates)', fakeAsync(() => {
    login(UserRole.WORKER);
    tick(250);
    const update = { id: '1', role: UserRole.WORKER, isCertified: true };
    FakeEventSource.instances[0].emit('account-update', update);
    expect(workerService.updateCache).toHaveBeenCalledWith(jasmine.objectContaining({ isCertified: true }));
    expect(clientService.updateCache).not.toHaveBeenCalled();
  }));

  it('live account updates reach the client cache', fakeAsync(() => {
    login(UserRole.CLIENT);
    tick(250);
    FakeEventSource.instances[0].emit('account-update', { id: '1', role: UserRole.CLIENT });
    expect(clientService.updateCache).toHaveBeenCalled();
    expect(workerService.updateCache).not.toHaveBeenCalled();
  }));

  it('password reset requests send form data', () => {
    service.requestPasswordReset('a@venus.test').subscribe();
    const req = httpMock.expectOne(`${auth}/reset-password`);
    expect((req.request.body as FormData).get('email')).toBe('a@venus.test');
    req.flush('ok');
  });
});
