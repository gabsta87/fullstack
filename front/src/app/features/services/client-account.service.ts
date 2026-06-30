import {Injectable, NgZone} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {BehaviorSubject, firstValueFrom, Observable, of} from 'rxjs';
import {environment} from "../../../environments/environment";
import {tap} from "rxjs/operators";
import {ClientPrivateAccount} from "../models/user.model";
@Injectable({ providedIn: 'root' })
export class ClientAccountService {
  private base = `${environment.apiBase}/account`;
  private accountSubject = new BehaviorSubject<ClientPrivateAccount | null>(null);

  constructor(private http: HttpClient, private zone : NgZone) {}

  getCurrentAccount(): Observable<ClientPrivateAccount> {
    const current = this.accountSubject.value;
    if (current) return of(current);

    // 🌟 Plus de withCredentials ! L'intercepteur gère le token.
    return this.http.get<ClientPrivateAccount>(`${this.base}/me`).pipe(
      tap(account => this.accountSubject.next(account))
    );
  }

  clearCache() {
    this.accountSubject.next(null);
  }

  listenToMyAccount(): Observable<ClientPrivateAccount> {
    return new Observable<ClientPrivateAccount>(observer => {
      if (this.accountSubject.value) {
        observer.next(this.accountSubject.value);
      }

      const token = localStorage.getItem('auth_token');
      const eventSource = new EventSource(`${this.base}/stream?token=${token}`);

      eventSource.addEventListener('account-update', (event: MessageEvent) => {
        this.zone.run(() => {
          const updatedAccount = JSON.parse(event.data);
          this.accountSubject.next(updatedAccount);
          observer.next(updatedAccount);
        });
      });

      eventSource.onerror = (error) => {
        this.zone.run(() => observer.error(error));
      };

      return () => eventSource.close();
    });
  }

  // SETTINGS
  async updateSettings(data: ClientPrivateAccount): Promise<any> {
    return await firstValueFrom(this.http.patch(`${this.base}/data`, data));
  }

  // LANGUAGE
  getCurrentUserLanguage(): string {
    const current = this.accountSubject.value;
    return current ? current.language : "EN";
  }

  async setCurrentUserLanguage(lang: string): Promise<any> {
    return await firstValueFrom(this.http.patch(`${this.base}/language`, { lang }));
  }

  // 🎯 FAVORITES (Maintenant 100% fonctionnel car le token est intercepté !)
  async addFavorite(workerId: string): Promise<any> {
    return await firstValueFrom(this.http.post(`${this.base}/favorites/${workerId}`, {}));
  }

  async removeFavorite(workerId: string): Promise<any> {
    return await firstValueFrom(this.http.delete(`${this.base}/favorites/${workerId}`));
  }
}
