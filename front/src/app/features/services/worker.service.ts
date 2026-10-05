// src/app/features/services/worker.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import {firstValueFrom, Observable, of, shareReplay} from 'rxjs';
import { catchError } from 'rxjs/operators';
import { WorkerMinimalProfile, WorkerPublicFullProfile } from '../models/user.model';
import {environment} from "../../../environments/environment";
import {GalleryFilters, } from "../models/filter.model";
import {Service} from "../models/common.model";

@Injectable({ providedIn: 'root' })
export class WorkerService {

  private readonly baseUrl = `${environment.apiBase}/workers`;

  constructor(private http: HttpClient) {}

  // ── Gallery ────────────────────────────────────────────────────────────────

  getGalleryPage(page: number, filters: GalleryFilters): Observable<WorkerMinimalProfile[]> {
    let params = new HttpParams().set('page', page.toString());

    // 🎯 Boucle dynamique sur toutes les clés de l'objet de filtres
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '' && value !== -1) {
        if (Array.isArray(value)) {
          // Pour les tableaux (services, languages) : on ajoute chaque élément au même paramètre
          value.forEach(val => {
            params = params.append(key, val.toString());
          });
        } else {
          params = params.set(key, value.toString());
        }
      }
    });

    return this.http.get<WorkerMinimalProfile[]>(`${this.baseUrl}`, { params })
      .pipe(catchError(() => of([])));
  }

  // ── Profile ────────────────────────────────────────────────────────────────

  getProfile(workerId: string): Observable<WorkerPublicFullProfile> {
    return this.http.get<WorkerPublicFullProfile>(`${this.baseUrl}/${workerId}`);
  }

  prefetchProfile(workerId: string): void {
    if (this.profileCache.has(workerId)) return;
    this.getProfile(workerId)
      .pipe(catchError(() => of(null)))
      .subscribe(profile => { if (profile) this.profileCache.set(workerId, profile); });
  }

  getCachedProfile(workerId: string): WorkerPublicFullProfile | null {
    return this.profileCache.get(workerId) ?? null;
  }

  private servicesCache$?: Observable<Service[]>;

  getWorkersServices(): Promise<Service[]> {
    if (!this.servicesCache$) {
      this.servicesCache$ = this.http.get<Service[]>(`${environment.apiBase}/common/services`).pipe(
        shareReplay(1) // 👈 Conserve la réponse en cache pour tous les abonnements futurs
      );
    }
    return firstValueFrom(this.servicesCache$);
  }

  private profileCache = new Map<string, WorkerPublicFullProfile>();
}
