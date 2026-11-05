import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { NgZone } from '@angular/core';
import { AuthService } from './auth.service';
import { WorkerAccountService } from './worker-account.service';
import { ClientAccountService } from './client-account.service';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;
  let workerServiceSpy: jasmine.SpyObj<WorkerAccountService>;
  let clientServiceSpy: jasmine.SpyObj<ClientAccountService>;

  beforeEach(() => {
    const routerMock = jasmine.createSpyObj('Router', ['navigate']);
    const workerMock = jasmine.createSpyObj('WorkerAccountService', ['clearCache', 'updateCache']);
    const clientMock = jasmine.createSpyObj('ClientAccountService', ['clearCache', 'updateCache']);

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerMock },
        { provide: WorkerAccountService, useValue: workerMock },
        { provide: ClientAccountService, useValue: clientMock },
        NgZone
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    workerServiceSpy = TestBed.inject(WorkerAccountService) as jasmine.SpyObj<WorkerAccountService>;
    clientServiceSpy = TestBed.inject(ClientAccountService) as jasmine.SpyObj<ClientAccountService>;

    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('devrait être créé', () => {
    expect(service).toBeTruthy();
  });

  it('devrait stocker le token, mettre à jour l\'état et retourner l\'utilisateur lors d\'un login réussi', (done) => {
    const mockLoginResponse = {
      token: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWI5OiJ0ZXN0QHZlbnVzLmNvbSIsInJvbGUiOiJST0xFX0NMSUVOVCJ9.signature',
      user: { id: '123', email: 'test@venus.com', role: 'CLIENT' }
    };

    service.login('test@venus.com', 'Password123!').subscribe(user => {
      expect(user).toEqual(mockLoginResponse.user as any);
      expect(localStorage.getItem('auth_token')).toEqual(mockLoginResponse.token);
      expect(service.isAuthenticated).toBeTrue();
      expect(service.getUser()).toEqual(mockLoginResponse.user as any);
      done();
    });

    const req = httpMock.expectOne(`${environment.apiBase}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'test@venus.com', password: 'Password123!' });
    req.flush(mockLoginResponse);
  });

  it('devrait nettoyer le stockage local et les caches lors du logout', (done) => {
    localStorage.setItem('auth_token', 'fake-token');

    service.logout().subscribe(() => {
      expect(localStorage.getItem('auth_token')).toBeNull();
      expect(service.isAuthenticated).toBeFalse();
      expect(service.getUser()).toBeNull();
      expect(workerServiceSpy.clearCache).toHaveBeenCalled();
      expect(clientServiceSpy.clearCache).toHaveBeenCalled();
      done();
    });

    const req = httpMock.expectOne(`${environment.apiBase}/auth/logout`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('devrait gérer l\'échec de session-check en déclenchant un logout local', (done) => {
    service.checkSession().subscribe(isValid => {
      expect(isValid).toBeFalse();
      expect(service.isAuthenticated).toBeFalse();
      done();
    });

    const req = httpMock.expectOne(`${environment.apiBase}/auth/session-check`);
    expect(req.request.method).toBe('GET');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
  });
});
