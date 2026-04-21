import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { Injectable } from '@angular/core';
import { tap, catchError } from 'rxjs/operators';
import {environment} from "../../../environments/environment";

@Injectable({ providedIn: 'root' })
export class AuthService {
  private baseUrl = `${environment.apiBase}/auth`;
  private redirectUrl = '';
  private sessionCache: { value: boolean; expires: number } | null = null;

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  constructor(private http: HttpClient) {}

  login(pseudo: string, password: string, redirectUrl: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/login`, { pseudo, password, redirectUrl }, { withCredentials: true }).pipe(
      tap(() => {
        this.isAuthenticatedSubject.next(true);
        this.sessionCache = { value: true, expires: Date.now() + 60_000 };
      })
    );
  }

  logout(): Observable<any> {
    return this.http.post(`${this.baseUrl}/logout`, {}, { withCredentials: true }).pipe(
      tap(() => {
        this.isAuthenticatedSubject.next(false);
        this.sessionCache = null;
      }),
      catchError((err) => {
        // Même en cas d'erreur (ex: 200 OK mais corps vide),
        // on force l'état déconnecté localement
        this.isAuthenticatedSubject.next(false);
        this.sessionCache = null;
        return of(null);
      })
    );
  }

  checkSession(): Observable<boolean> {
    // Return cached value if still fresh (60s)
    if (this.sessionCache && Date.now() < this.sessionCache.expires) {
      return of(this.sessionCache.value);
    }
    return this.http.get<boolean>(`${this.baseUrl}/session-check`,
      { withCredentials: true }).pipe(
      tap(isAuth => {
        this.isAuthenticatedSubject.next(isAuth);
        this.sessionCache = { value: isAuth, expires: Date.now() + 60_000 };
      }),
      catchError(() => {
        this.sessionCache = null;
        return of(false);
      })
    );
  }

  setRedirectUrl(url: string) { this.redirectUrl = url; }
  getRedirectUrl()            { return this.redirectUrl; }
}
