import {Injectable, NgZone} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {BehaviorSubject, firstValueFrom, Observable, of} from 'rxjs';
import {environment} from "../../../environments/environment";
import {tap} from "rxjs/operators";
import {ClientPrivateAccount} from "../models/user.model";

@Injectable({ providedIn: 'root' })
export class ClientAccountService {
  // 🎯 Changement de chemin vers le contrôleur spécifique Client
  private base = `${environment.apiBase}/account/client`;
  private accountSubject = new BehaviorSubject<ClientPrivateAccount | null>(null);

  constructor(private http: HttpClient) {}

  getCurrentAccount(): Observable<ClientPrivateAccount> {
    const current = this.accountSubject.value;
    if (current) return of(current);

    return this.http.get<ClientPrivateAccount>(`${this.base}/me`).pipe(
      tap(account => this.accountSubject.next(account))
    );
  }

  // Permet à l'AuthService de pousser les mises à jour SSE ici
  updateCache(account: ClientPrivateAccount) {
    this.accountSubject.next(account);
  }

  clearCache() {
    this.accountSubject.next(null);
  }

  // 🎯 Plus de fuite réseau ! On écoute simplement le BehaviorSubject local
  listenToMyAccount(): Observable<ClientPrivateAccount | null> {
    return this.accountSubject.asObservable();
  }

  async updateSettings(data: ClientPrivateAccount): Promise<any> {
    return await firstValueFrom(this.http.patch(`${this.base}/data`, data));
  }

  getCurrentUserLanguage(): string {
    const current = this.accountSubject.value;
    return current ? current.language : "EN";
  }

  async setCurrentUserLanguage(lang: string): Promise<any> {
    // 🎯 Correction du chemin : pointe vers le hub commun /account/language
    return await firstValueFrom(this.http.patch(`${environment.apiBase}/account/language`, { lang }));
  }

  async addFavorite(workerId: string): Promise<any> {
    return await firstValueFrom(this.http.post(`${this.base}/favorites/${workerId}`, {}));
  }

  async removeFavorite(workerId: string): Promise<any> {
    return await firstValueFrom(this.http.delete(`${this.base}/favorites/${workerId}`));
  }
}
