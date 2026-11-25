import { TestBed } from '@angular/core/testing';
import { HTTP_INTERCEPTORS, HttpClient, provideHttpClient, withInterceptorsFromDi, withXhr } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { isApiRequest, JwtInterceptor, NEW_TOKEN_HEADER, SITE_LANGUAGE_KEY, TOKEN_STORAGE_KEY } from './jwt.interceptor';
import { environment } from '../../../environments/environment';

describe('JwtInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  const api = environment.apiBase;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withXhr(), withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('adds the bearer token to API requests', () => {
    localStorage.setItem(TOKEN_STORAGE_KEY, 'abc.def.ghi');
    http.get(`${api}/account/client/me`).subscribe();
    const req = httpMock.expectOne(`${api}/account/client/me`);
    expect(req.request.headers.get('Authorization')).toBe('Bearer abc.def.ghi');
    req.flush({});
  });

  it('sends no Authorization header when logged out', () => {
    http.get(`${api}/workers`).subscribe();
    const req = httpMock.expectOne(`${api}/workers`);
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush([]);
  });

  it('never leaks the token to another host', () => {
    localStorage.setItem(TOKEN_STORAGE_KEY, 'secret-token');
    for (const url of ['https://evil.example.com/collect', 'https://api.stripe.com/v1/x', `https://evil.example.com${api}/x`]) {
      http.get(url).subscribe();
      const req = httpMock.expectOne(url);
      expect(req.request.headers.has('Authorization')).withContext(url).toBeFalse();
      req.flush({});
    }
  });

  it('sends the site language to the API only (used for the emails)', () => {
    localStorage.setItem(SITE_LANGUAGE_KEY, 'UA');
    http.post(`${api}/auth/forgot-password`, {}).subscribe();
    const req = httpMock.expectOne(`${api}/auth/forgot-password`);
    expect(req.request.headers.get('Accept-Language')).toBe('uk');
    req.flush({});

    http.get('https://cdn.example.com/x').subscribe();
    const external = httpMock.expectOne('https://cdn.example.com/x');
    expect(external.request.headers.has('Accept-Language')).toBeFalse();
    external.flush({});
  });

  it('stores the renewed token sent after a credential change', () => {
    localStorage.setItem(TOKEN_STORAGE_KEY, 'old');
    http.patch(`${api}/account/data`, {}).subscribe();
    httpMock.expectOne(`${api}/account/data`).flush({}, { headers: { [NEW_TOKEN_HEADER]: 'new-token' } });
    expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBe('new-token');
  });

  it('ignores a token header coming from a third party', () => {
    localStorage.setItem(TOKEN_STORAGE_KEY, 'mine');
    http.get('https://evil.example.com/x').subscribe();
    httpMock.expectOne('https://evil.example.com/x').flush({}, { headers: { [NEW_TOKEN_HEADER]: 'attacker' } });
    expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBe('mine');
  });

  it('isApiRequest matches only the API base', () => {
    expect(isApiRequest(`${api}/auth/login`)).toBeTrue();
    expect(isApiRequest(api)).toBeTrue();
    expect(isApiRequest(`${api}evil.com/x`)).toBeFalse();
    expect(isApiRequest('/assets/lang/data.json')).toBeFalse();
  });
});
