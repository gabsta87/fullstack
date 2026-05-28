// src/app/features/services/worker.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { WorkerSimpleProfile, WorkerFullProfile } from '../models/user.model';
import {environment} from "../../../environments/environment";
import {GalleryFilters} from "../models/filter.model";

@Injectable({ providedIn: 'root' })
export class WorkerService {

  private readonly baseUrl = `${environment.apiBase}/workers`;

  constructor(private http: HttpClient) {}

  // ── Gallery ────────────────────────────────────────────────────────────────

  getGalleryPage(page: number, filters: GalleryFilters): Observable<WorkerSimpleProfile[]> {
    let params = new HttpParams().set('page', page);
    if (filters.region)            params = params.set('region',    filters.region);
    if (filters.eyeColor)          params = params.set('eyeColor',  filters.eyeColor);
    if (filters.hairColor)         params = params.set('hairColor', filters.hairColor);
    if (filters.heightMin != null) params = params.set('heightMin', filters.heightMin);
    if (filters.heightMax != null) params = params.set('heightMax', filters.heightMax);
    if (filters.weightMin != null) params = params.set('weightMin', filters.weightMin);
    if (filters.weightMax != null) params = params.set('weightMax', filters.weightMax);
    if (filters.bodyType?.length)  params = params.set('bodyType',  filters.bodyType.join(','));
    if (filters.services?.length)  params = params.set('services',  filters.services.join(','));
    return this.http.get<WorkerSimpleProfile[]>(`${this.baseUrl}`, { params })
      .pipe(catchError(() => of([])));
  }

  /** Lazy hover preview fetch — UUID string */
  getPreviewThumbs(workerId: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/${workerId}/previews`)
      .pipe(catchError(() => of([])));
  }

  // ── Profile ────────────────────────────────────────────────────────────────

  getProfile(workerId: string): Observable<WorkerFullProfile> {
    return this.http.get<WorkerFullProfile>(`${this.baseUrl}/${workerId}`);
  }

  prefetchProfile(workerId: string): void {
    if (this.profileCache.has(workerId)) return;
    this.getProfile(workerId)
      .pipe(catchError(() => of(null)))
      .subscribe(profile => { if (profile) this.profileCache.set(workerId, profile); });
  }

  getCachedProfile(workerId: string): WorkerFullProfile | null {
    return this.profileCache.get(workerId) ?? null;
  }

  getWorkersServices() {
    return this.http.get<string[]>(`${this.baseUrl}/services`);
  }

  private profileCache = new Map<string, WorkerFullProfile>();
}
