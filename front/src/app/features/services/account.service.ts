import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { WorkerGalleryDTO } from '../models/worker.model';

export interface AccountMe {
  id: string;
  username: string;
  email: string;
  role: 'WORKER' | 'CLIENT';
  // worker-only
  available?: boolean;
  region?: string;
  location?: string;
  bodyType?: string;
  height?: number;
  weight?: number;
  description?: string;
  services?: string[];
  mainThumbUrl?: string;
  subscriptionDaysLeft?: number;
  expired?: boolean;
}

export interface SettingsUpdate {
  username?: string;
  email?: string;
  password?: string;
}

export interface WorkerProfileUpdate {
  description?: string;
  location?: string;
  region?: string;
  height?: number;
  weight?: number;
  bodyType?: string;
  services?: string[];
}

@Injectable({ providedIn: 'root' })
export class AccountService {
  private base = 'http://localhost:8080/account';

  constructor(private http: HttpClient) {}

  getMe(): Observable<AccountMe> {
    return this.http.get<AccountMe>(`${this.base}/me`, { withCredentials: true });
  }

  updateSettings(data: SettingsUpdate): Observable<any> {
    return this.http.patch(`${this.base}/settings`, data, { withCredentials: true });
  }

  // CLIENT
  getFavorites(): Observable<WorkerGalleryDTO[]> {
    return this.http.get<WorkerGalleryDTO[]>(`${this.base}/favorites`, { withCredentials: true });
  }

  addFavorite(workerId: string): Observable<any> {
    return this.http.post(`${this.base}/favorites/${workerId}`, {}, { withCredentials: true });
  }

  removeFavorite(workerId: string): Observable<any> {
    return this.http.delete(`${this.base}/favorites/${workerId}`, { withCredentials: true });
  }

  // WORKER
  setAvailability(available: boolean): Observable<any> {
    return this.http.patch(`${this.base}/worker/availability`, { available }, { withCredentials: true });
  }

  updateProfile(data: WorkerProfileUpdate): Observable<any> {
    return this.http.patch(`${this.base}/worker/profile`, data, { withCredentials: true });
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
