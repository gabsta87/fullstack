import {Injectable, NgZone} from '@angular/core';
import {BehaviorSubject, firstValueFrom, Observable, of} from "rxjs";
import {WorkerFullProfile, WorkerPrivateAccount, WorkerProfileUpdate} from "../models/user.model";
import {HttpClient} from "@angular/common/http";
import {environment} from "../../../environments/environment";
import {tap} from "rxjs/operators";

@Injectable({
  providedIn: 'root'
})
export class WorkerAccountService {

  private base = `${environment.apiBase}/account`;

  private accountSubject = new BehaviorSubject<WorkerPrivateAccount | null>(null);

  constructor(private http: HttpClient,private zone: NgZone) { }

  getCurrentAccount(): Observable<WorkerPrivateAccount> {
    const current = this.accountSubject.value;
    if (current) {
      return of(current);
    }

    return this.http.get<WorkerPrivateAccount>(`${this.base}/me`, { withCredentials: true }).pipe(
      tap(account => this.accountSubject.next(account))
    );
  }

  listenToMyAccount(): Observable<WorkerPrivateAccount> {
    return new Observable<WorkerPrivateAccount>(observer => {

      // 1 — On distribue IMMÉDIATEMENT les données déjà chargées par le Resolver
      if (this.accountSubject.value) {
        observer.next(this.accountSubject.value);
      }

      // 2 — On ouvre la connexion temps réel pour les futures modifications
      const eventSource = new EventSource(`${this.base}/worker/stream`, { withCredentials: true });

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

  clearCache() {
    this.accountSubject.next(null);
  }

  setAvailability(available: boolean): Observable<any> {
    return this.http.patch(`${this.base}/worker/availability`, { available }, { withCredentials: true });
  }

  updateProfile(data: WorkerProfileUpdate): Observable<any> {
    return this.http.patch(`${this.base}/worker/profile`, data, { withCredentials: true });
  }

  updateServices(services: string[]): Observable<WorkerFullProfile> {
    console.log("updateServices : updating ", services);
    return this.http.patch<WorkerFullProfile>(`${this.base}/worker/updateservices`, services, { withCredentials: true });
  }

  uploadPhoto(file: File, title?: string): Observable<any> {
    const fd = new FormData();
    console.log("uploadPhoto : ", file);
    fd.append('file', file);
    if (title) fd.append('title', title);
    console.log("uploadPhoto : ", fd);
    return this.http.post(`${this.base}/worker/photos`, fd, { withCredentials: true });
  }

  async deletePhoto(photoId: string): Promise<void> {
    await firstValueFrom(this.http.delete(`${this.base}/worker/photos/${photoId}`, { withCredentials: true }));
  }

  setMainPhoto(photoId: string): Observable<any> {
    return this.http.patch(`${this.base}/worker/photos/${photoId}/main`, {}, { withCredentials: true });
  }

  reorderPhotos(orderedIds: string[]): Observable<any> {
    return this.http.patch(`${this.base}/worker/photos/reorder`, orderedIds, { withCredentials: true });
  }
}
