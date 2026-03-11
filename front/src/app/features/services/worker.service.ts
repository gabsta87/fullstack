import { Injectable } from '@angular/core';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { GalleryFilters, WorkerGalleryDTO, WorkerProfile } from '../models/worker.model';

@Injectable({ providedIn: 'root' })
export class WorkerService {

  // In dev, Spring serves everything on 8080 via DevMediaController.
  // In prod, change this to your domain — Nginx proxies /api/** to Spring.
  private readonly apiBase = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  // ── Gallery ────────────────────────────────────────────────────────────────

  /**
   * Fetches a page of gallery cards.
   * mainThumbUrl is included. previewThumbUrls are NOT — fetched lazily on hover.
   */
  getGalleryPage(page: number, filters: GalleryFilters): Observable<WorkerGalleryDTO[]> {
    let params = new HttpParams().set('page', page);

    if (filters.region)             params = params.set('region',    filters.region);
    if (filters.eyeColor)           params = params.set('eyeColor',  filters.eyeColor);
    if (filters.hairColor)          params = params.set('hairColor', filters.hairColor);
    if (filters.heightMin != null)  params = params.set('heightMin', filters.heightMin);
    if (filters.heightMax != null)  params = params.set('heightMax', filters.heightMax);
    if (filters.weightMin != null)  params = params.set('weightMin', filters.weightMin);
    if (filters.weightMax != null)  params = params.set('weightMax', filters.weightMax);
    if (filters.bodyType?.length)   params = params.set('bodyType',  filters.bodyType.join(','));
    if (filters.services?.length)   params = params.set('services',  filters.services.join(','));

    return this.http.get<WorkerGalleryDTO[]>(`${this.apiBase}/workers`, { params })
      .pipe(catchError(() => of([])));
  }

  /**
   * Lazily fetches preview thumbnails for one worker.
   * Called only when the user hovers a gallery card.
   */
  getPreviewThumbs(workerId: number): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiBase}/workers/${workerId}/previews`)
      .pipe(catchError(() => of([])));
  }

  // ── Profile ────────────────────────────────────────────────────────────────

  /** Full profile — called when the user navigates to a profile page. */
  getProfile(workerId: number): Observable<WorkerProfile> {
    return this.http.get<WorkerProfile>(`${this.apiBase}/workers/${workerId}`);
  }

  /**
   * Prefetches the full profile in the background while hover preview plays.
   * Stores result in a simple in-memory map so the profile page loads instantly.
   */
  prefetchProfile(workerId: number): void {
    if (this.profileCache.has(workerId)) return;
    this.getProfile(workerId)
      .pipe(catchError(() => of(null)))
      .subscribe(profile => {
        if (profile) this.profileCache.set(workerId, profile);
      });
  }

  getCachedProfile(workerId: number): WorkerProfile | null {
    return this.profileCache.get(workerId) ?? null;
  }

  private profileCache = new Map<number, WorkerProfile>();
}
