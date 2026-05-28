import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, of} from 'rxjs';
import {environment} from "../../../environments/environment";
import {tap} from "rxjs/operators";
import {ClientPrivateAccount,} from "../models/user.model";

@Injectable({ providedIn: 'root' })
export class ClientAccountService {
  private base = `${environment.apiBase}/account`;

  private currentAccount: ClientPrivateAccount | null = null;

  constructor(private http: HttpClient) {}

  getCurrentAccount(): Observable<ClientPrivateAccount> {
    if (this.currentAccount) {
      return of(this.currentAccount);
    }
    return this.http.get<ClientPrivateAccount>(`${this.base}/me`, { withCredentials: true }).pipe(
      tap(account => this.currentAccount = account)
    );
  }

  clearCache() {
    this.currentAccount = null;
  }

  updateSettings(data: ClientPrivateAccount): Observable<any> {
    return this.http.patch(`${this.base}/settings`, data, { withCredentials: true });
  }

  getCurrentUserLanguage(): string {
    if(!this.currentAccount)
      return "EN";
    return this.currentAccount?.language;
  }

  setCurrentUserLanguage(lang: string): Observable<any> {
    return this.http.patch(`${this.base}/language`, { lang }, { withCredentials: true });
  }

  // CLIENT
  addFavorite(workerId: string): Observable<any> {
    return this.http.post(`${this.base}/favorites/${workerId}`, {}, { withCredentials: true });
  }

  removeFavorite(workerId: string): Observable<any> {
    return this.http.delete(`${this.base}/favorites/${workerId}`, { withCredentials: true });
  }

}
