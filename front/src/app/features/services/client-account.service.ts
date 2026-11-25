import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {BehaviorSubject, firstValueFrom, Observable, of} from 'rxjs';
import {environment} from "../../../environments/environment";
import {distinctUntilChanged, finalize, map, tap} from "rxjs/operators";
import {ClientProfile} from "../models/user.model";
import {GalleryFilters} from "../models/filter.model";

@Injectable({ providedIn: 'root' })
export class ClientAccountService {
  // Client-specific controller
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

  updateCache(account: ClientProfile) {
    this.accountSubject.next(account);
  }

  clearCache() {
    this.accountSubject.next(null);
  }

  listenToMyAccount(): Observable<ClientProfile | null> {
    return this.accountSubject.asObservable();
  }

  private loadingAccount = false;

  /**
   * Loads the client account once, when not already cached.
   * Only for a logged-in CLIENT.
   */
  ensureLoaded(): void {
    if (this.accountSubject.value || this.loadingAccount) return;
    this.loadingAccount = true;
    this.getCurrentAccount()
      .pipe(finalize(() => this.loadingAccount = false))
      .subscribe({ error: (err) => console.error('Impossible de charger le compte client', err) });
  }

  /** true when the worker is one of the client's favorites (updated live). */
  isFavorite$(workerId: string): Observable<boolean> {
    return this.accountSubject.pipe(
      map(account => account?.favorites?.some(f => f.id === workerId) ?? false),
      distinctUntilChanged()
    );
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

  async reportAccount(reason: string, workerId: string): Promise<any> {
    return await firstValueFrom(this.http.post(`${this.base}/report-account`, {reason, workerId}));
  }

  async postComment(workerId: string, content: string): Promise<any> {
    return await firstValueFrom(this.http.post(`${this.base}/comment`, { workerId, content }));
  }

  async deleteComment(commentId: number): Promise<void> {
    await firstValueFrom(this.http.delete(`${this.base}/comment/${commentId}`));
  }
}
