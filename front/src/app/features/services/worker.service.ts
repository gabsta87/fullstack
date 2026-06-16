// src/app/features/services/worker.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import {firstValueFrom, Observable, of} from 'rxjs';
import { catchError } from 'rxjs/operators';
import { WorkerSimpleProfile, WorkerFullProfile } from '../models/user.model';
import {environment} from "../../../environments/environment";
import {GalleryFilters, GeographicZone} from "../models/filter.model";

@Injectable({ providedIn: 'root' })
export class WorkerService {

  private readonly baseUrl = `${environment.apiBase}/workers`;

  constructor(private http: HttpClient) {}

  // ── Gallery ────────────────────────────────────────────────────────────────

  getGalleryPage(page: number, filters: GalleryFilters): Observable<WorkerSimpleProfile[]> {
    let params = new HttpParams().set('page', page);
    if (filters.username)          params = params.set('username',  filters.username);
    if (filters.zoneId)            params = params.set('zoneId',    filters.zoneId);
    if (filters.eyeColor)          params = params.set('eyeColor',  filters.eyeColor);
    if (filters.hairColor)         params = params.set('hairColor', filters.hairColor);
    if (filters.bodyType)          params = params.set('bodyType',  filters.bodyType);
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

  async getGeographicZones(): Promise<GeographicZone[]> {
    return firstValueFrom(this.http.get<GeographicZone[]>(`${this.baseUrl}/locations`));
  }

  private profileCache = new Map<string, WorkerFullProfile>();
}
