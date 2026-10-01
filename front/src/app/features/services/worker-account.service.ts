import {Injectable} from '@angular/core';
import {BehaviorSubject, firstValueFrom, Observable, of} from "rxjs";
import {HttpClient} from "@angular/common/http";
import {tap} from "rxjs/operators";
import {environment} from "../../../environments/environment";
import {WorkerPrivateAccount, WorkerProfileUpdate} from "../models/user.model";
@Injectable({ providedIn: 'root' })
export class WorkerAccountService {
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

  listenToMyAccount(): Observable<WorkerPrivateAccount | null> {
    return this.accountSubject.asObservable();
  }

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

  async updateServices(services: number[]): Promise<WorkerPrivateAccount> {
    const updatedAccount = await firstValueFrom(
      this.http.patch<WorkerPrivateAccount>(`${this.base}/updateservices`, services)
    );
    this.accountSubject.next(updatedAccount);
    return updatedAccount;
  }

  async uploadMedia(files: File[]): Promise<any> {
    const fd = new FormData();
    files.forEach(file => {
      fd.append('files', file, file.name);
    });
    const result = await firstValueFrom(this.http.post(`${this.base}/media`, fd));
    return result;
  }

  async deletePhoto(photoId: string): Promise<void> {
    console.log("Deleting photo with ID:", photoId);
    await firstValueFrom(this.http.delete(`${this.base}/photos/${photoId}`));
  }

  async setMainPhoto(photoId: string): Promise<any> {
    return await firstValueFrom(this.http.patch(`${this.base}/photos/${photoId}/main`, {}));
  }

  async reorderPhotos(orderedIds: string[]): Promise<any> {
    return await firstValueFrom(this.http.patch(`${this.base}/photos/reorder`, orderedIds));
  }

  async deleteVideo(videoId: string): Promise<void> {
    await firstValueFrom(this.http.delete(`${this.base}/videos/${videoId}`));
  }

  async requestCertification(): Promise<WorkerPrivateAccount> {
    const updatedAccount = await firstValueFrom(
      this.http.get<WorkerPrivateAccount>(`${this.base}/request-certification`)
    );
    this.accountSubject.next(updatedAccount);
    return updatedAccount;
  }

  async uploadCertificationPhoto(formData: FormData): Promise<WorkerPrivateAccount> {
    const updatedAccount = await firstValueFrom(
      this.http.post<WorkerPrivateAccount>(`${this.base}/certification-photo`, formData)
    );
    this.accountSubject.next(updatedAccount);
    return updatedAccount;
  }
}
