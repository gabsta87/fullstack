import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import {environment} from "../../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = `${environment.apiBase}/api/admin`;

  // Mock temporaire pour simuler l'admin connecté (à lier idéalement à ton AuthService plus tard)
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

  // ── ANNONCEURS / WORKERS ──────────────────────────────────────────────────

  getAllProfiles(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/profiles`, { headers: this.getAdminHeaders() });
  }

  toggleStatus(workerId: string): Observable<{ success: boolean; isActive: boolean }> {
    return this.http.post<{ success: boolean; isActive: boolean }>(
      `${this.apiUrl}/profiles/${workerId}/toggle-status`,
      {},
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
