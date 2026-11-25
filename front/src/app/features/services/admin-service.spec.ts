import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminService } from './admin-service';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';
import { FakeEventSource } from '../../../testing/fake-event-source';
import { BehaviorSubject, of } from 'rxjs';
import { fakeAsync, tick } from '@angular/core/testing';
import { UserRole } from '../models/roles';

describe('AdminService.loadCertificationPhoto', () => {
  let service: AdminService;
  let httpMock: HttpTestingController;
  let restore: () => void;

  beforeEach(() => {
    restore = FakeEventSource.install();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withXhr()),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { isAuthenticated$: of(true), getUser: () => null } },
      ],
    });
    service = TestBed.inject(AdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    restore();
  });

  it('downloads private photos through the API and returns an object URL', async () => {
    const url = '/admin/certification-photos/w1/0f8fad5b-d9cb-469f-a165-70867728950e.jpg';
    const promise = service.loadCertificationPhoto(url);
    const req = httpMock.expectOne(`${environment.apiBase}${url}`);
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['x'], { type: 'image/jpeg' }));
    const result = await promise;
    expect(result.startsWith('blob:')).toBeTrue();
    URL.revokeObjectURL(result);
  });

  it('returns legacy public URLs untouched and empty for missing photos', async () => {
    expect(await service.loadCertificationPhoto('/media/w1/certification/a.jpg')).toBe('/media/w1/certification/a.jpg');
    expect(await service.loadCertificationPhoto(null)).toBe('');
    httpMock.expectNone(() => true);
  });
});

describe('AdminService live updates (SSE)', () => {
  let service: AdminService;
  let httpMock: HttpTestingController;
  let restore: () => void;
  let auth$: BehaviorSubject<boolean>;
  let user: any;
  const api = `${environment.apiBase}/admin`;

  beforeEach(() => {
    restore = FakeEventSource.install();
    localStorage.setItem('auth_token', 'admin-token');
    auth$ = new BehaviorSubject<boolean>(true);
    user = { role: UserRole.ADMIN };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withXhr()),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { isAuthenticated$: auth$, getUser: () => user } },
      ],
    });
    service = TestBed.inject(AdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    restore();
    localStorage.removeItem('auth_token');
  });

  it('reloads the users once when other admins change accounts, and the audit log on any action', fakeAsync(() => {
    const seen: number[] = [];
    service.users$.subscribe(users => seen.push(users.length));
    service.auditLogs$.subscribe();
    httpMock.expectOne(`${api}/users`).flush([{}]);
    httpMock.expectOne(`${api}/logs`).flush([]);

    const stream = FakeEventSource.instances[0];
    stream.emit('USERS_UPDATED', 'a');
    stream.emit('USERS_UPDATED', 'b');
    stream.emit('CERTIFICATION_UPDATED', 1);
    stream.emit('AUDIT_LOG_UPDATED', 'BAN');
    tick(1000);
    httpMock.expectOne(`${api}/users`).flush([{}, {}]);
    httpMock.expectOne(`${api}/logs`).flush([]);
    httpMock.match(`${api}/certification-requests`).forEach(r => r.flush([]));
    expect(seen).toEqual([1, 2]);
  }));

  it('reopens the stream after a logout and a new admin login', () => {
    expect(FakeEventSource.instances.length).toBe(1);
    auth$.next(false);
    expect(FakeEventSource.instances[0].closed).toBeTrue();
    auth$.next(true);
    expect(FakeEventSource.instances.length).toBe(2);
  });

  it('never opens the admin stream for another role', () => {
    auth$.next(false);
    user = { role: UserRole.WORKER };
    auth$.next(true);
    expect(FakeEventSource.instances.length).toBe(1);
  });
});
