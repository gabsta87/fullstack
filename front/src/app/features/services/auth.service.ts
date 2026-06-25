import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, map, Observable, of, throwError } from 'rxjs';
import { Injectable, NgZone } from '@angular/core';
import { catchError, tap } from 'rxjs/operators';
import { environment } from "../../../environments/environment";
import { BaseUser } from "../models/user.model";
import { Router } from "@angular/router";
import { ClientAccountService } from "./client-account.service";
import { WorkerAccountService } from "./worker-account.service";

// Interface locale pour correspondre au format du backend Spring Boot
interface LoginResponse {
  token: string;
  user: BaseUser;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private baseUrl = `${environment.apiBase}/auth`;
  private accountUrl = `${environment.apiBase}/account`;
  private sessionCache: { value: boolean; expires: number } | null = null;
  private redirectUrl = '';
  private currentAccount : BaseUser | null = null;

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  private eventSource: EventSource | null = null;

  constructor(
    private http: HttpClient,
    private router : Router,
    private workerService : WorkerAccountService,
    private clientService : ClientAccountService,
    private zone: NgZone
  ) {}

  login(email: string, password: string): Observable<BaseUser> {
    this.setRedirectUrl("");

    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, { email, password }).pipe(
      tap((response) => {
        localStorage.setItem('auth_token', response.token);

        this.currentAccount = response.user;
        this.isAuthenticatedSubject.next(true);

        setTimeout(() => {
          console.log("Démarrage du flux SSE avec le Token JWT...");
          this.establishRealTimeStream();
        }, 200);
      }),
      // On extrait uniquement le 'user' pour que la méthode continue de renvoyer un Observable<BaseUser>
      map(response => response.user)
    );
  }

  checkSession(): Observable<boolean> {
    if (this.sessionCache && Date.now() < this.sessionCache.expires) {
      return of(this.sessionCache.value);
    }

    return this.http.get<BaseUser>(`${this.baseUrl}/session-check`).pipe(
      tap((user) => {
        console.log("Token JWT valide. Session et profil restaurés !");

        this.currentAccount = user;
        this.isAuthenticatedSubject.next(true);

        this.sessionCache = { value: true, expires: Date.now() + 60_000 };
        this.establishRealTimeStream();
      }),
      map(() => true),
      catchError((error) => {
        console.warn("Session expirée ou Token invalide.");
        this.handleLocalLogout();
        return of(false);
      })
    );
  }

  logout(): Observable<any> {
    return this.http.post(`${this.baseUrl}/logout`, {}).pipe(
      tap(() => {
        this.handleLocalLogout();
      }),
      catchError((err) => {
        this.handleLocalLogout();
        return of(null);
      })
    );
  }

  private establishRealTimeStream() {
    if (this.eventSource) return;
    console.log(`Establishing connection to ${this.accountUrl}/stream`);

    const token = localStorage.getItem('auth_token');

    // 1. Création du flux
    this.eventSource = new EventSource(`${this.accountUrl}/stream?token=${token}`);

    // 2. Écoute de l'événement custom de déconnexion forcée par le serveur
    this.eventSource.addEventListener('session-expired', (event: MessageEvent) => {
      this.zone.run(() => {
        console.warn("Session invalidée à distance par le serveur :", event.data);
        this.handleLocalLogout();
        this.router.navigate(['/login']);
      });
    });

    // 🎯 3. GESTION DES INTERRUPTIONS ET ERREURS
    this.eventSource.onerror = (error) => {
      this.zone.run(() => {
        console.error("Le flux SSE a rencontré une erreur ou a été interrompu.");

        // Cas A : Le navigateur a abandonné (readyState = CLOSED)
        // Souvent parce que le serveur a renvoyé un 401 (Token expiré)
        if (this.eventSource?.readyState === EventSource.CLOSED) {
          console.warn("Flux définitivement fermé par le serveur. Tentative de ré-authentification...");

          this.closeStream(); // On nettoie l'ancien flux défectueux

          // On appelle notre checkSession() solidifié :
          // Si le token est encore bon, il va recréer un flux tout propre.
          // Si le token est expiré, il va déconnecter proprement l'utilisateur.
          this.checkSession().subscribe();
        }

          // Cas B : Le readyState est CONNECTING.
          // C'est une simple coupure réseau (Wi-Fi, 4G).
        // On ne fait RIEN : le navigateur est déjà en train de tenter de se reconnecter tout seul.
        else if (this.eventSource?.readyState === EventSource.CONNECTING) {
          console.log("Tentative de reconnexion automatique par le navigateur (problème réseau)...");
        }
      });
    };
  }

// 🎯 Petite méthode utilitaire indispensable pour nettoyer proprement
  public closeStream() {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
      console.log("Flux SSE fermé proprement.");
    }
  }

  private handleLocalLogout() {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }

    localStorage.removeItem('auth_token');

    this.isAuthenticatedSubject.next(false);
    this.sessionCache = null;
    this.currentAccount = null;
    this.closeStream()

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
