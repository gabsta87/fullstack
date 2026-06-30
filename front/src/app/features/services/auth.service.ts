import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { tap, map, catchError } from 'rxjs/operators';
import { environment } from "../../../environments/environment";
import { BaseUser } from '../models/user.model';
import { ClientAccountService } from './client-account.service';
import { WorkerAccountService } from './worker-account.service';

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

  // 🎯 Nouvelle méthode pour lire le contenu du JWT sans appel API
  public getDecodedToken(): any {
    const token = localStorage.getItem('auth_token');
    if (!token) return null;
    try {
      return JSON.parse(atob(token.split('.')[1]));
    } catch (e) {
      return null;
    }
  }

  login(email: string, password: string): Observable<BaseUser> {
    this.setRedirectUrl("");
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, { email, password }).pipe(
      tap((response) => {
        localStorage.setItem('auth_token', response.token);
        this.currentAccount = response.user;
        this.isAuthenticatedSubject.next(true);

        setTimeout(() => this.establishRealTimeStream(), 200);
      }),
      map(response => response.user)
    );
  }

  checkSession(): Observable<boolean> {
    if (this.sessionCache && Date.now() < this.sessionCache.expires) {
      return of(this.sessionCache.value);
    }

    return this.http.get<BaseUser>(`${this.baseUrl}/session-check`).pipe(
      tap((user) => {
        this.currentAccount = user;
        this.isAuthenticatedSubject.next(true);
        this.sessionCache = { value: true, expires: Date.now() + 60_000 };
        this.establishRealTimeStream();
      }),
      map(() => true),
      catchError(() => {
        this.handleLocalLogout();
        return of(false);
      })
    );
  }

  logout(): Observable<any> {
    return this.http.post(`${this.baseUrl}/logout`, {}).pipe(
      tap(() => this.handleLocalLogout()),
      catchError(() => {
        this.handleLocalLogout();
        return of(null);
      })
    );
  }

  private establishRealTimeStream() {
    if (this.eventSource) return;
    const token = localStorage.getItem('auth_token');

    // Connexion sur le hub générique d'écoute
    this.eventSource = new EventSource(`${this.accountUrl}/stream?token=${token}`);

    // 🎯 CENTRALISATION : L'unique écouteur dispatch l'événement au bon service
    this.eventSource.addEventListener('account-update', (event: MessageEvent) => {
      this.zone.run(() => {
        const updatedAccount = JSON.parse(event.data);
        const tokenData = this.getDecodedToken();

        // On aiguille la mise à jour selon le rôle inscrit dans le JWT
        if (tokenData?.role === 'ROLE_CLIENT') {
          this.clientService.updateCache(updatedAccount);
        } else if (tokenData?.role === 'ROLE_WORKER') {
          this.workerService.updateCache(updatedAccount);
        }
      });
    });

    this.eventSource.addEventListener('session-expired', (event: MessageEvent) => {
      this.zone.run(() => {
        this.handleLocalLogout();
        this.router.navigate(['/login']);
      });
    });

    this.eventSource.onerror = () => {
      this.zone.run(() => {
        if (this.eventSource?.readyState === EventSource.CLOSED) {
          this.closeStream();
          this.checkSession().subscribe();
        }
      });
    };
  }

  public closeStream() {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }

  private handleLocalLogout() {
    this.closeStream();
    localStorage.removeItem('auth_token');
    this.isAuthenticatedSubject.next(false);
    this.sessionCache = null;
    this.currentAccount = null;
    this.workerService.clearCache();
    this.clientService.clearCache();
  }

  get isAuthenticated(): boolean { return this.isAuthenticatedSubject.value; }
  getUser(): BaseUser | null { return this.currentAccount; }
  setRedirectUrl(url: string) { this.redirectUrl = url; }
}
