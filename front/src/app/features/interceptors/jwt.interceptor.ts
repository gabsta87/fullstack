import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export const TOKEN_STORAGE_KEY = 'auth_token';
/** Sent by the backend after a password / email change, which revokes the previous token. */
export const NEW_TOKEN_HEADER = 'X-Auth-Token';

/** True only for requests to our own API: the token must never be sent to third parties. */
export function isApiRequest(url: string): boolean {
  const base = environment.apiBase.replace(/\/+$/, '');
  return url === base || url.startsWith(base + '/');
}

@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!isApiRequest(request.url)) {
      return next.handle(request);
    }

    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (token) {
      request = request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
    }

    return next.handle(request).pipe(
      tap(event => {
        if (event instanceof HttpResponse) {
          const renewed = event.headers.get(NEW_TOKEN_HEADER);
          if (renewed) {
            localStorage.setItem(TOKEN_STORAGE_KEY, renewed);
          }
        }
      })
    );
  }
}
