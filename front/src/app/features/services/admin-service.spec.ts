import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminService } from './admin-service';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';
import { FakeEventSource } from '../../../testing/fake-event-source';
import { of } from 'rxjs';

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
