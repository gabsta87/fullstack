import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from "../../../environments/environment";
import {WorkerFullProfile} from "../models/user.model";

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = `${environment.apiBase}/admin`;

  constructor(private http: HttpClient) {}

  getUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/users`);
  }

  // ── ANNONCEURS / WORKERS ──────────────────────────────────────────────────

  getWorkers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/profiles`);
  }

  setLockedStatus(workerId: string, lock: boolean): Observable<WorkerFullProfile> {
    const params = new HttpParams().set('lock', lock.toString());

    return this.http.post<WorkerFullProfile>(`${this.apiUrl}/profiles/${workerId}/set-locked`, {}, {params: params});
  }

  updateWorkerStatus(workerId: string, statusPayload: { locked?: boolean; banned?: boolean; hidden?: boolean }): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/profiles/${workerId}/status`, statusPayload);
  }

  updateDaysCredit(workerId: string, newDaysValue: number, reason: string): Observable<{ success: boolean; newDaysValue: number }> {
    const payload = { workerId, newDaysValue, reason };
    return this.http.post<{ success: boolean; newDaysValue: number }>(`${this.apiUrl}/profiles/update-days`, payload);
  }

  // ── CERTIFICATIONS ────────────────────────────────────────────────────────

  verifyCertification(workerId: string, approved: boolean, rejectionReason: string): Observable<{ success: boolean }> {
    const payload = { workerId, approved, rejectionReason };
    return this.http.post<{ success: boolean }>(`${this.apiUrl}/profiles/verify-certification`, payload);
  }

  // ── AUDIT LOGS ────────────────────────────────────────────────────────────

  getAuditLogs(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/logs`);
  }

  // ── FUTURES FONCTIONNALITÉS ACCESSIBLES FACILEMENT ────────────────────────

  inviteAdmin(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/admins/invite`, { email });
  }

  updateRegion(regionData: {id:number,name:string,parentId:number}): Observable<any> {
    return this.http.put(`${this.apiUrl}/region/`, regionData);
  }

  deleteRegion(regionId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/regions/${regionId}`);
  }

  updateService(serviceData:{id: number, name: string, description?:string}): Observable<any> {
    return this.http.put(`${this.apiUrl}/service/`, serviceData);
  }

  deleteService(serviceId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/services/${serviceId}`);
  }

  updateLegalText(key: string, content: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/legal`, { key, content });
  }
}
