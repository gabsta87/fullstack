import {HttpClient} from '@angular/common/http';
import {BehaviorSubject, Observable, of, throwError} from 'rxjs';
import {Injectable, NgZone} from '@angular/core';
import {catchError, tap} from 'rxjs/operators';
import {environment} from "../../../environments/environment";
import {BaseUser} from "../models/user.model";
import {Router} from "@angular/router";
import {ClientAccountService} from "./client-account.service";
import {WorkerAccountService} from "./worker-account.service";

@Injectable({ providedIn: 'root' })
export class AuthService {
  private baseUrl = `${environment.apiBase}/auth`;
  private accountUrl = `${environment.apiBase}/account`; // Url vers les streams
  private sessionCache: { value: boolean; expires: number } | null = null;
  private redirectUrl = '';
  private currentAccount : BaseUser | null = null;

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  // Instance unique du flux SSE pour l'authentification
  private eventSource: EventSource | null = null;

  constructor(
    private http: HttpClient,
    private router : Router,
    private workerService : WorkerAccountService,
    private clientService : ClientAccountService,
    private zone: NgZone
  ) {}

  login(pseudo: string, password: string): Observable<BaseUser> {
    this.setRedirectUrl("");
    return this.http.post<BaseUser>(`${this.baseUrl}/login`, { pseudo, password }, { withCredentials: true }).pipe(
      tap((user) => {
        this.currentAccount = user;
        this.isAuthenticatedSubject.next(true);

        // 🚀 CONNEXION TEMPS RÉEL IMMÉDIATE
        this.establishRealTimeStream();
      })
    );
  }

  checkSession(): Observable<boolean> {
    if (this.sessionCache && Date.now() < this.sessionCache.expires) {
      return of(this.sessionCache.value);
    }

    return this.http.get<boolean>(`${this.baseUrl}/session-check`, { withCredentials: true }).pipe(
      tap(() => {
        console.log("Session active");
        this.isAuthenticatedSubject.next(true);
        this.sessionCache = { value: true, expires: Date.now() + 60_000 };

        this.establishRealTimeStream();
      }),
      catchError((error) => {
        this.handleLocalLogout();
        if (error.status === 401) {
          return of(false);
        }
        return throwError(() => error);
      })
    );
  }

  logout(): Observable<any> {
    return this.http.post(`${this.baseUrl}/logout`, {}, { withCredentials: true }).pipe(
      tap(() => {
        this.handleLocalLogout();
      }),
      catchError((err) => {
        this.handleLocalLogout();
        return of(null);
      })
    );
  }

  /**
   * Initialise la connexion SSE pour suivre l'état de la session
   */
  private establishRealTimeStream() {
    if (this.eventSource) return; // Évite les doublons

    console.log(`establishing connexion to ${this.accountUrl}/stream`)
    this.eventSource = new EventSource(`${this.accountUrl}/stream`, { withCredentials: true });

    // 🚨 Écoute d'une invalidation de session poussée par le serveur
    this.eventSource.addEventListener('session-expired', (event: MessageEvent) => {
      this.zone.run(() => {
        console.warn("Session invalidée à distance par le serveur :", event.data);
        this.handleLocalLogout();
        this.router.navigate(['/login']);
      });
    });

    this.eventSource.onerror = (error) => {
      console.error("Erreur ou déconnexion du flux SSE d'authentification", error);
      this.handleLocalLogout();
    };
  }

  /**
   * Centralise le nettoyage local lors d'une déconnexion (manuelle ou forcée)
   */
  private handleLocalLogout() {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
    this.isAuthenticatedSubject.next(false);
    this.sessionCache = null;
    this.currentAccount = null;

    this.workerService.clearCache();
    this.clientService.clearCache();
  }

  get isAuthenticated(): boolean {
    return this.isAuthenticatedSubject.value;
  }

  getUser(): BaseUser | null {
    return this.currentAccount;
  }

  setRedirectUrl(url: string) {
    this.redirectUrl = url;
  }
}
