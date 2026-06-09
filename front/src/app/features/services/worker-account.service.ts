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
      const eventSource = new EventSource(`${this.base}/stream`, { withCredentials: true });

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

  async setAvailability(available: boolean): Promise<any> {
    const updatedAccount = await firstValueFrom(
      this.http.patch<WorkerPrivateAccount>(
        `${this.base}/worker/availability`,
        { available },
        { withCredentials: true }
      )
    );

    this.accountSubject.next(updatedAccount);

    return updatedAccount;
  }

  async updateProfile(data: WorkerProfileUpdate): Promise<any> {
    await firstValueFrom(this.http.patch(`${this.base}/worker/profile`, data, { withCredentials: true }))
  }

  // SERVICES

  async updateServices(services: string[]): Promise<any> {
    await firstValueFrom(this.http.patch<WorkerFullProfile>(`${this.base}/worker/updateservices`, services, { withCredentials: true }))
  }

  // PHOTOS

  async uploadPhoto(file: File, title?: string): Promise<any> {
    const fd = new FormData();
    console.log("uploadPhoto : ", file, " to ",this.base,"/worker/photos}");
    fd.append('file', file);
    if (title) fd.append('title', title);
    return await firstValueFrom(this.http.post(`${this.base}/worker/photos`, fd, { withCredentials: true }));
  }

  async deletePhoto(photoId: string): Promise<void> {
    await firstValueFrom(this.http.delete(`${this.base}/worker/photos/${photoId}`, { withCredentials: true }));
  }

  async setMainPhoto(photoId: string): Promise<any> {
    await firstValueFrom(this.http.patch(`${this.base}/worker/photos/${photoId}/main`, {}, { withCredentials: true }));
  }

  async reorderPhotos(orderedIds: string[]): Promise<any> {
    await firstValueFrom(this.http.patch(`${this.base}/worker/photos/reorder`, orderedIds, { withCredentials: true }));
  }
}
