import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { ClientAccountService } from './client-account.service';

describe('ClientAccountService — availability alerts', () => {
  let service: ClientAccountService;
  let httpMock: HttpTestingController;
  const base = `${environment.apiBase}/account/client/availability-alerts`;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(withXhr()), provideHttpClientTesting()] });
    service = TestBed.inject(ClientAccountService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lists the profiles waited for', async () => {
    const result = service.getAvailabilityAlerts();
    httpMock.expectOne(base).flush(['w1']);
    expect(await result).toEqual(['w1']);
  });

  it('subscribes with PUT and unsubscribes with DELETE', async () => {
    const on = service.setAvailabilityAlert('w1', true);
    const put = httpMock.expectOne(`${base}/w1`);
    expect(put.request.method).toBe('PUT');
    put.flush(null, { status: 204, statusText: 'No Content' });
    await on;

    const off = service.setAvailabilityAlert('w/1', false);
    const del = httpMock.expectOne(`${base}/w%2F1`);
    expect(del.request.method).toBe('DELETE');
    del.flush(null, { status: 204, statusText: 'No Content' });
    await off;
  });
});
