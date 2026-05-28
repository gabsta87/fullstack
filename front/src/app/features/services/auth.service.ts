import {HttpClient} from '@angular/common/http';
import {BehaviorSubject, Observable, of} from 'rxjs';
import {Injectable} from '@angular/core';
import {catchError, tap} from 'rxjs/operators';
import {environment} from "../../../environments/environment";
import {ClientPrivateAccount, WorkerPrivateAccount} from "../models/user.model";

@Injectable({ providedIn: 'root' })
export class AuthService {
  private baseUrl = `${environment.apiBase}/auth`;
  private sessionCache: { value: boolean; expires: number } | null = null;

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  constructor(private http: HttpClient) {}

  login(pseudo: string, password: string): Observable<WorkerPrivateAccount | ClientPrivateAccount> {
    return this.http.post<WorkerPrivateAccount | ClientPrivateAccount>(`${this.baseUrl}/login`, { pseudo, password }, { withCredentials: true }).pipe(
      tap(() => {
        this.isAuthenticatedSubject.next(true);
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

}
