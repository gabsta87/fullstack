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
    if (current) {
      return of(current);
    }
    return this.http.get<ClientPrivateAccount>(`${this.base}/me`, { withCredentials: true }).pipe(
      tap(account => this.accountSubject.next(account))
    );
  }

  clearCache() {
    this.accountSubject.next(null);
  }

  listenToMyAccount(): Observable<ClientPrivateAccount> {
    return new Observable<ClientPrivateAccount>(observer => {

      // 1 — On distribue IMMÉDIATEMENT les données déjà chargées par le Resolver
      if (this.accountSubject.value) {
        observer.next(this.accountSubject.value);
      }

      // 2 — On ouvre la connexion temps réel pour les futures modifications
      const eventSource = new EventSource(`${this.base}/client/stream`, { withCredentials: true });

      eventSource.addEventListener('account-update', (event: MessageEvent) => {
        this.zone.run(() => {
          const updatedAccount = JSON.parse(event.data);

          // On met à jour le BehaviorSubject central pour garder le cache synchro
          this.accountSubject.next(updatedAccount);

          // On pousse la mise à jour à l'IHM
          observer.next(updatedAccount);
        });
      });

      eventSource.onerror = (error) => {
        this.zone.run(() => observer.error(error));
      };

      // Nettoyage à la destruction du composant
      return () => eventSource.close();
    });
  }

  // SETTINGS

  async updateSettings(data: ClientPrivateAccount): Promise<any> {
    console.log("updateSettings : ", data);
    return await firstValueFrom(this.http.patch(`${this.base}/data`, data, { withCredentials: true }));
  }

  // LANGUAGE

  getCurrentUserLanguage(): string {
    const current = this.accountSubject.value;
    if (!current) return "EN";
    return current.language;
  }

  async setCurrentUserLanguage(lang: string): Promise<any> {
    return await firstValueFrom(this.http.patch(`${this.base}/language`, { lang }, { withCredentials: true }));
  }

  // FAVORITES

  async addFavorite(workerId: string): Promise<any> {
    return await firstValueFrom(this.http.post(`${this.base}/favorites/${workerId}`, {}, { withCredentials: true }));
  }

  async removeFavorite(workerId: string): Promise<any> {
    return await firstValueFrom(this.http.delete(`${this.base}/favorites/${workerId}`, { withCredentials: true }));
  }

}
