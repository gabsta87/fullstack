import { Injectable } from '@angular/core';
import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';
import { Observable } from 'rxjs';
import {environment} from "../../../environments/environment";
import {WorkerFullProfile} from "../models/user.model";

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = `${environment.apiBase}/api/admin`;

  private adminId = "ADMIN-UUID-1234";
  private adminUsername = "ChefModo_Venus";

  constructor(private http: HttpClient) {}

  /**
   * Génère les en-têtes d'administration requis par le backend pour l'audit log
   */
  private getAdminHeaders(): HttpHeaders {
    return new HttpHeaders({
      'X-Admin-Id': this.adminId,
      'X-Admin-Username': this.adminUsername
    });
  }

  getUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/users`, { headers: this.getAdminHeaders() });
  }

  // ── ANNONCEURS / WORKERS ──────────────────────────────────────────────────

  getWorkers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/profiles`, { headers: this.getAdminHeaders() });
  }

  setLockedStatus(workerId: string, lock: boolean): Observable<WorkerFullProfile> {
    const params = new HttpParams().set('lock', lock.toString());

    return this.http.post<WorkerFullProfile>(
      `${this.apiUrl}/profiles/${workerId}/set-locked`,
      {}, // Body
      {
        headers: this.getAdminHeaders(),
        params: params
      }
    );
  }

  updateWorkerStatus(workerId: string, statusPayload: { locked?: boolean; banned?: boolean; hidden?: boolean }): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}/profiles/${workerId}/status`,
      statusPayload,
      { headers: this.getAdminHeaders() }
    );
  }

  updateDaysCredit(workerId: string, newDaysValue: number, reason: string): Observable<{ success: boolean; newDaysValue: number }> {
    const payload = { workerId, newDaysValue, reason };
    return this.http.post<{ success: boolean; newDaysValue: number }>(
      `${this.apiUrl}/profiles/update-days`,
      payload,
      { headers: this.getAdminHeaders() }
    );
  }

  // ── CERTIFICATIONS ────────────────────────────────────────────────────────

  verifyCertification(workerId: string, approved: boolean, rejectionReason: string): Observable<{ success: boolean }> {
    const payload = { workerId, approved, rejectionReason };
    return this.http.post<{ success: boolean }>(
      `${this.apiUrl}/profiles/verify-certification`,
      payload,
      { headers: this.getAdminHeaders() }
    );
  }

  // ── AUDIT LOGS ────────────────────────────────────────────────────────────

  getAuditLogs(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/logs`, { headers: this.getAdminHeaders() });
  }

  // ── FUTURES FONCTIONNALITÉS ACCESSIBLES FACILEMENT ────────────────────────

  inviteAdmin(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/admins/invite`, { email }, { headers: this.getAdminHeaders() });
  }

  deleteRegion(regionId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/regions/${regionId}`, { headers: this.getAdminHeaders() });
  }

  updateLegalText(key: string, content: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/legal`, { key, content }, { headers: this.getAdminHeaders() });
  }
}
