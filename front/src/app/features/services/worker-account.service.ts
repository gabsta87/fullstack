import {Injectable} from '@angular/core';
import {Observable} from "rxjs";
import {WorkerFullProfile, WorkerPrivateAccount, WorkerProfileUpdate} from "../models/user.model";
import {HttpClient} from "@angular/common/http";
import {environment} from "../../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class WorkerAccountService {

  private base = `${environment.apiBase}/account`;
  private currentAccount$: Observable<WorkerPrivateAccount> | null = null;

  constructor(private http: HttpClient) { }

  getCurrentAccount(): Observable<WorkerPrivateAccount> {
    if (!this.currentAccount$) {
      this.currentAccount$ = this.http.get<WorkerPrivateAccount>(`${this.base}/me`, { withCredentials: true });
    }
    return this.currentAccount$;
  }

  // WORKER
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
    fd.append('file', file);
    if (title) fd.append('title', title);
    return this.http.post(`${this.base}/worker/photos`, fd, { withCredentials: true });
  }

  deletePhoto(photoId: string): Observable<any> {
    return this.http.delete(`${this.base}/worker/photos/${photoId}`, { withCredentials: true });
  }

  setMainPhoto(photoId: string): Observable<any> {
    return this.http.patch(`${this.base}/worker/photos/${photoId}/main`, {}, { withCredentials: true });
  }

  reorderPhotos(orderedIds: string[]): Observable<any> {
    return this.http.patch(`${this.base}/worker/photos/reorder`, orderedIds, { withCredentials: true });
  }
}
