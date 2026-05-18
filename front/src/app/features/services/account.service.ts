import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {map, Observable, of} from 'rxjs';
import { WorkerSimpleProfile } from '../models/worker.model';
import {environment} from "../../../environments/environment";
import {tap} from "rxjs/operators";

export interface CurrentProfile {
  id: string;
  username: string;
  email: string;
  language: string;
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

export interface UserProfile {
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
  private base = `${environment.apiBase}/account`;

  private currentAccount: CurrentProfile | null = null;

  constructor(private http: HttpClient) {}

  getCurrentProfile(): Observable<CurrentProfile> {
    if (this.currentAccount) {
      return of(this.currentAccount);
    }
    return this.http.get<CurrentProfile>(`${this.base}/me`, { withCredentials: true }).pipe(
      tap(account => this.currentAccount = account)
    );
  }

  updateSettings(data: UserProfile): Observable<any> {
    return this.http.patch(`${this.base}/settings`, data, { withCredentials: true });
  }

  getCurrentUserLanguage(): Observable<string> {
    return this.getCurrentProfile().pipe(
      map(account => account.language)
    );
  }

  setCurrentUserLanguage(lang: string): Observable<any> {
    return this.http.patch(`${this.base}/language`, { lang }, { withCredentials: true });
  }

  // CLIENT
  getFavorites(): Observable<WorkerSimpleProfile[]> {
    return this.http.get<WorkerSimpleProfile[]>(`${this.base}/favorites`, { withCredentials: true });
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
