import {HttpClient} from '@angular/common/http';
import {BehaviorSubject, map, Observable, of, throwError} from 'rxjs';
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
        setTimeout(() => {
          console.log("Démarrage du flux SSE après enregistrement du cookie...");
          this.establishRealTimeStream();
        }, 200);
      })
    );
  }

  checkSession(): Observable<boolean> {
    if (this.sessionCache && Date.now() < this.sessionCache.expires) {
      return of(this.sessionCache.value);
    }

    // On attend un retour vide <void>
    return this.http.get<void>(`${this.baseUrl}/session-check`, { withCredentials: true }).pipe(
      map(() => true),
      tap(() => {
        console.log("Session active");
        this.isAuthenticatedSubject.next(true);
        this.sessionCache = { value: true, expires: Date.now() + 60_000 };

        this.establishRealTimeStream();
      }),
      catchError((error) => {
        this.handleLocalLogout();
        if (error.status === 401) {
          return of(false); // 🎯 Si le serveur répond 401, on renvoie 'false' proprement
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
    if (this.eventSource) return;

    console.log(`establishing connexion to ${this.accountUrl}/stream`)
    this.eventSource = new EventSource(`${this.accountUrl}/stream`, { withCredentials: true });

    // ✅ CAS 1 : Le serveur valide explicitement que la session est morte
    this.eventSource.addEventListener('session-expired', (event: MessageEvent) => {
      this.zone.run(() => {
        console.warn("Session invalidée à distance par le serveur :", event.data);
        this.handleLocalLogout();
        this.router.navigate(['/login']);
      });
    });

    // ✅ CAS 2 : Simple coupure réseau (ex: tunnel, mise en arrière-plan)
    this.eventSource.onerror = (error) => {
      // On laisse le navigateur tenter de se reconnecter tout seul en tâche de fond.
      console.warn("Flux SSE interrompu temporairement. Reconnexion automatique en cours...");
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
