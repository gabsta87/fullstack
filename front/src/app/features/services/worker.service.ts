// src/app/features/services/worker.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { GalleryFilters, WorkerGalleryDTO, WorkerProfile } from '../models/worker.model';
import {environment} from "../../../environments/environment";

@Injectable({ providedIn: 'root' })
export class WorkerService {

  private readonly baseUrl = `${environment.apiBase}/workers`;

  constructor(private http: HttpClient) {}

  // ── Gallery ────────────────────────────────────────────────────────────────

  getGalleryPage(page: number, filters: GalleryFilters): Observable<WorkerGalleryDTO[]> {
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
    return this.http.get<WorkerGalleryDTO[]>(`${this.baseUrl}`, { params })
      .pipe(catchError(() => of([])));
  }

  /** Lazy hover preview fetch — UUID string */
  getPreviewThumbs(workerId: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/${workerId}/previews`)
      .pipe(catchError(() => of([])));
  }

  // ── Profile ────────────────────────────────────────────────────────────────

  getProfile(workerId: string): Observable<WorkerProfile> {
    return this.http.get<WorkerProfile>(`${this.baseUrl}/${workerId}`);
  }

  prefetchProfile(workerId: string): void {
    if (this.profileCache.has(workerId)) return;
    this.getProfile(workerId)
      .pipe(catchError(() => of(null)))
      .subscribe(profile => { if (profile) this.profileCache.set(workerId, profile); });
  }

  getCachedProfile(workerId: string): WorkerProfile | null {
    return this.profileCache.get(workerId) ?? null;
  }

  getWorkers(page: number, size: number, filters: GalleryFilters): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    // Région (chaîne simple)
    if (filters.region) {
      params = params.set('region', filters.region);
    }

    // BodyType (Tableau -> conversion en chaîne séparée par des virgules)
    if (filters.bodyType && filters.bodyType.length > 0) {
      // Si c'est déjà une string (cas particulier), on l'envoie telle quelle
      // Sinon on joint le tableau
      const bodyTypeValue = Array.isArray(filters.bodyType)
        ? filters.bodyType.join(',')
        : filters.bodyType;

      params = params.set('bodyType', bodyTypeValue);
    }

    // Services (Tableau -> conversion en chaîne)
    if (filters.services && filters.services.length > 0) {
      params = params.set('services', filters.services.join(','));
    }

    return this.http.get<any>(`${environment.apiBase}/workers`, { params });
  }

  getWorkersServices() {
    return this.http.get<string[]>(`${environment.apiBase}/workers/services`);
  }

  private profileCache = new Map<string, WorkerProfile>();
}
