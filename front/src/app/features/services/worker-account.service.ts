import { Injectable, NgZone } from '@angular/core';
import { BehaviorSubject, firstValueFrom, Observable, of } from "rxjs";
import { HttpClient } from "@angular/common/http";
import { tap } from "rxjs/operators";
import { environment } from "../../../environments/environment";
import { WorkerPrivateAccount, WorkerProfileUpdate } from "../models/user.model";

@Injectable({
  providedIn: 'root'
})
export class WorkerAccountService {
  private base = `${environment.apiBase}/account`;
  private accountSubject = new BehaviorSubject<WorkerPrivateAccount | null>(null);

  // 🎯 Flux public pour lire l'état actuel n'importe où sans relancer de flux SSE
  public account$ = this.accountSubject.asObservable();

  constructor(private http: HttpClient, private zone: NgZone) { }

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
      // Émission immédiate de la valeur en cache si elle existe
      if (this.accountSubject.value) {
        observer.next(this.accountSubject.value);
      }

      const token = localStorage.getItem('auth_token');
      const eventSource = new EventSource(`${environment.apiBase}/account/stream?token=${token}`);

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

      // 🧼 Nettoyage automatique : ferme le flux SSE quand le composant Angular est détruit
      return () => eventSource.close();
    });
  }

  clearCache() {
    this.accountSubject.next(null);
  }

  async setAvailability(available: boolean): Promise<WorkerPrivateAccount> {
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

  async updateProfileData(payload: any): Promise<WorkerPrivateAccount> {
    console.log("updateProfileData : ", payload);
    const updatedAccount = await firstValueFrom(
      this.http.patch<WorkerPrivateAccount>(`${this.base}/data`, payload, { withCredentials: true })
    );
    // 🎯 Correction : On pousse la mise à jour ici aussi !
    this.accountSubject.next(updatedAccount);
    return updatedAccount;
  }

  async updateProfile(data: WorkerProfileUpdate): Promise<WorkerPrivateAccount> {
    const updatedAccount = await firstValueFrom(
      this.http.patch<WorkerPrivateAccount>(`${this.base}/worker/profile`, data, { withCredentials: true })
    );
    this.accountSubject.next(updatedAccount);
    return updatedAccount;
  }

  async updateServices(services: string[]): Promise<WorkerPrivateAccount> {
    const updatedAccount = await firstValueFrom(
      this.http.patch<WorkerPrivateAccount>(`${this.base}/worker/updateservices`, services, { withCredentials: true })
    );
    this.accountSubject.next(updatedAccount);
    return updatedAccount;
  }

  async uploadPhoto(file: File, title?: string): Promise<any> {
    const fd = new FormData();
    fd.append('file', file);
    if (title) fd.append('title', title);
    return await firstValueFrom(this.http.post(`${this.base}/worker/photos`, fd, { withCredentials: true }));
  }

  async deletePhoto(photoId: string): Promise<void> {
    await firstValueFrom(this.http.delete(`${this.base}/worker/photos/${photoId}`, { withCredentials: true }));
  }

  async setMainPhoto(photoId: string): Promise<any> {
    return await firstValueFrom(this.http.patch(`${this.base}/worker/photos/${photoId}/main`, {}, { withCredentials: true }));
  }

  async reorderPhotos(orderedIds: string[]): Promise<any> {
    return await firstValueFrom(this.http.patch(`${this.base}/worker/photos/reorder`, orderedIds, { withCredentials: true }));
  }
}
