import {HttpClient} from '@angular/common/http';
import {BehaviorSubject, Observable, of, throwError} from 'rxjs';
import {Injectable} from '@angular/core';
import {catchError, tap} from 'rxjs/operators';
import {environment} from "../../../environments/environment";
import {BaseUser} from "../models/user.model";
import {Router} from "@angular/router";
import {ClientAccountService} from "./client-account.service";
import {WorkerAccountService} from "./worker-account.service";

@Injectable({ providedIn: 'root' })
export class AuthService {
  private baseUrl = `${environment.apiBase}/auth`;
  private sessionCache: { value: boolean; expires: number } | null = null;
  private redirectUrl = '';
  private currentAccount : BaseUser | null = null;

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  constructor(private http: HttpClient, private router : Router, private workerService : WorkerAccountService, private clientService : ClientAccountService) {}

  login(pseudo: string, password: string): Observable<BaseUser> {
    this.setRedirectUrl("");
    return this.http.post<BaseUser>(`${this.baseUrl}/login`, { pseudo, password }, { withCredentials: true }).pipe(
      tap((user) => {
        this.currentAccount = user;
        this.isAuthenticatedSubject.next(true);

        console.log("Logged as user. Role received:", user.role);
        console.log("Redirect url : ",this.redirectUrl)

        if (this.redirectUrl) {
          this.router.navigateByUrl(this.redirectUrl);
          this.redirectUrl = '';
        } else if (user.role === "WORKER") {
          console.log("Redirecting to profile management");
          this.router.navigateByUrl("/profile-management");
        } else if (user.role === "CLIENT") {
          console.log("Redirecting to account management");
          this.router.navigateByUrl("/account");
        } else {
          console.error("Unknown role, cannot redirect:", user.role);
        }
      }),
      catchError((err) => {
        console.error("DEBUG LOGIN - Erreur reçue du serveur :", err);
        // Il est crucial de voir le status (err.status) et le message (err.error)
        throw err; // On relance l'erreur pour que le composant puisse l'afficher
      })
    );
  }

  getUser() : BaseUser | null {
    return this.currentAccount
  }

  logout(): Observable<any> {
    return this.http.post(`${this.baseUrl}/logout`, {}, { withCredentials: true }).pipe(
      tap(() => {
        this.isAuthenticatedSubject.next(false);
        this.sessionCache = null;

        this.workerService.clearCache();
        this.clientService.clearCache();
      }),
      catchError((err) => {
        this.isAuthenticatedSubject.next(false);
        this.sessionCache = null;
        return of(null);
      })
    );
  }

  get isAuthenticated(): boolean {
    return this.isAuthenticatedSubject.value;
  }

  checkSession(): Observable<boolean> {
    if (this.sessionCache && Date.now() < this.sessionCache.expires) {
      return of(this.sessionCache.value);
    }

    return this.http.get<boolean>(`${this.baseUrl}/session-check`, { withCredentials: true }).pipe(
      tap(isAuth => {
        this.isAuthenticatedSubject.next(isAuth);
        this.sessionCache = { value: isAuth, expires: Date.now() + 60_000 };
      }),
      catchError((error) => {
        if (error.status === 401) {
          console.warn("Session expirée (401)");
          this.isAuthenticatedSubject.next(false);
          this.sessionCache = null;
          return of(false); // On renvoie false, le Guard redirigera vers login
        }

        console.error("Erreur serveur/réseau :", error.status);
        this.sessionCache = null;

        return throwError(() => error);
      })
    );
  }

  setRedirectUrl(url: string) { this.redirectUrl = url; }
  getRedirectUrl()            { return this.redirectUrl; }
}
