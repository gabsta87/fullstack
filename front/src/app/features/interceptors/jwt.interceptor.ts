import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export const TOKEN_STORAGE_KEY = 'auth_token';
/** Sent by the backend after a password / email change, which revokes the previous token. */
export const NEW_TOKEN_HEADER = 'X-Auth-Token';
/** Same key as LanguageManagerService: the site language, used by the backend for emails. */
export const SITE_LANGUAGE_KEY = 'site_lang';

/** ISO 639-1 code of the site language ('UA' is the site's code for Ukrainian). */
export function siteLanguageTag(): string {
  const code = (localStorage.getItem(SITE_LANGUAGE_KEY) || 'FR').toLowerCase();
  return code === 'ua' ? 'uk' : code;
}

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

    const headers: Record<string, string> = { 'Accept-Language': siteLanguageTag() };
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    request = request.clone({ setHeaders: headers });

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
