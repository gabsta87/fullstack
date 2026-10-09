import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {BehaviorSubject, firstValueFrom, Observable, of} from 'rxjs';
import {environment} from "../../../environments/environment";
import {tap} from "rxjs/operators";
import {ClientProfile} from "../models/user.model";
import {GalleryFilters} from "../models/filter.model";

@Injectable({ providedIn: 'root' })
export class ClientAccountService {
  // 🎯 Changement de chemin vers le contrôleur spécifique Client
  private base = `${environment.apiBase}/account/client`;
  private accountSubject = new BehaviorSubject<ClientProfile | null>(null);

  constructor(private http: HttpClient) {}

  getCurrentAccount(): Observable<ClientProfile> {
    const current = this.accountSubject.value;
    if (current) return of(current);

    return this.http.get<ClientProfile>(`${this.base}/me`).pipe(
      tap(account => this.accountSubject.next(account))
    );
  }

  // Permet à l'AuthService de pousser les mises à jour SSE ici
  updateCache(account: ClientProfile) {
    this.accountSubject.next(account);
  }

  clearCache() {
    this.accountSubject.next(null);
  }

  // 🎯 Plus de fuite réseau ! On écoute simplement le BehaviorSubject local
  listenToMyAccount(): Observable<ClientProfile | null> {
    return this.accountSubject.asObservable();
  }

  async updateSettings(data: ClientProfile): Promise<any> {
    return await firstValueFrom(this.http.patch(`${this.base}/data`, data));
  }

  async setCurrentUserLanguage(lang: string): Promise<any> {
    return await firstValueFrom(this.http.patch(`${environment.apiBase}/account/language`, { lang }));
  }

  async addFavorite(workerId: string): Promise<any> {
    const updatedClient = await firstValueFrom(this.http.post<ClientProfile>(`${this.base}/favorites/${workerId}`, {}));
    this.updateCache(updatedClient);
    return updatedClient;
  }

  async removeFavorite(workerId: string): Promise<any> {
    const updatedClient = await firstValueFrom(this.http.delete<ClientProfile>(`${this.base}/favorites/${workerId}`));
    this.updateCache(updatedClient);
    return updatedClient;
  }

  async getSavedPreferences(): Promise<GalleryFilters>{
    return await firstValueFrom(this.http.get(`${this.base}/filters`));
  }
}
