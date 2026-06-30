import { Injectable, NgZone } from '@angular/core';
import { BehaviorSubject, firstValueFrom, Observable, of } from "rxjs";
import { HttpClient } from "@angular/common/http";
import { tap } from "rxjs/operators";
import { environment } from "../../../environments/environment";
import { WorkerPrivateAccount, WorkerProfileUpdate } from "../models/user.model";

@Injectable({ providedIn: 'root' })
export class WorkerAccountService {
  // 🎯 Changement de chemin vers le contrôleur spécifique Worker
  private base = `${environment.apiBase}/account/worker`;
  private accountSubject = new BehaviorSubject<WorkerPrivateAccount | null>(null);

  constructor(private http: HttpClient) { }

  getCurrentAccount(): Observable<WorkerPrivateAccount> {
    const current = this.accountSubject.value;
    if (current) return of(current);

    return this.http.get<WorkerPrivateAccount>(`${this.base}/me`).pipe(
      tap(account => this.accountSubject.next(account))
    );
  }

  updateCache(account: WorkerPrivateAccount) {
    this.accountSubject.next(account);
  }

  clearCache() {
    this.accountSubject.next(null);
  }

  // 🎯 Plus de fuite réseau ici non plus !
  listenToMyAccount(): Observable<WorkerPrivateAccount | null> {
    return this.accountSubject.asObservable();
  }

  // 🎯 Correction des chemins ci-dessous pour éviter le bug "/worker/worker/..."
  async setAvailability(available: boolean): Promise<WorkerPrivateAccount> {
    const updatedAccount = await firstValueFrom(
      this.http.patch<WorkerPrivateAccount>(`${this.base}/availability`, { available }));
    this.accountSubject.next(updatedAccount);
    return updatedAccount;
  }

  async updateProfileData(payload: any): Promise<WorkerPrivateAccount> {
    const updatedAccount = await firstValueFrom(
      this.http.patch<WorkerPrivateAccount>(`${this.base}/data`, payload)
    );
    this.accountSubject.next(updatedAccount);
    return updatedAccount;
  }

  async updateProfile(data: WorkerProfileUpdate): Promise<WorkerPrivateAccount> {
    const updatedAccount = await firstValueFrom(
      this.http.patch<WorkerPrivateAccount>(`${this.base}/profile`, data)
    );
    this.accountSubject.next(updatedAccount);
    return updatedAccount;
  }

  async updateServices(services: string[]): Promise<WorkerPrivateAccount> {
    const updatedAccount = await firstValueFrom(
      this.http.patch<WorkerPrivateAccount>(`${this.base}/updateservices`, services)
    );
    this.accountSubject.next(updatedAccount);
    return updatedAccount;
  }

  async uploadPhoto(file: File, title?: string): Promise<any> {
    const fd = new FormData();
    fd.append('file', file);
    if (title) fd.append('title', title);
    return await firstValueFrom(this.http.post(`${this.base}/photos`, fd));
  }

  async deletePhoto(photoId: string): Promise<void> {
    await firstValueFrom(this.http.delete(`${this.base}/photos/${photoId}`));
  }

  async setMainPhoto(photoId: string): Promise<any> {
    return await firstValueFrom(this.http.patch(`${this.base}/photos/${photoId}/main`, {}));
  }

  async reorderPhotos(orderedIds: string[]): Promise<any> {
    return await firstValueFrom(this.http.patch(`${this.base}/photos/reorder`, orderedIds));
  }
}
