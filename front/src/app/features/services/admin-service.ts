import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {firstValueFrom, Observable} from 'rxjs';
import {environment} from "../../../environments/environment";
import {WorkerFullProfile, WorkerProfileForAdmin} from "../models/user.model";
import {GeographicZone, GeographicZoneWithParent} from "../models/filter.model";
import {AdminLog, CertificationRequest} from "../models/common.model";

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = `${environment.apiBase}/admin`;

  constructor(private http: HttpClient) {}

  getUsers(): Observable<WorkerProfileForAdmin[]> {
    return this.http.get<WorkerProfileForAdmin[]>(`${this.apiUrl}/users`);
  }

  getWorkerProfileForAdmin(workerId: string): Observable<WorkerFullProfile> {
    return this.http.get<WorkerFullProfile>(`${this.apiUrl}/workers/${workerId}`);
  }

  // ── ANNONCEURS / WORKERS ──────────────────────────────────────────────────

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

  getCertificationRequests():Observable<CertificationRequest[]>{
    return this.http.get<any[]>(`${this.apiUrl}/certification-requests`);
  }

  verifyCertification(requestId: number, approved: boolean, rejectionReason: string): Observable<{ success: boolean }> {
    const payload = { requestId, approved, rejectionReason };
    return this.http.post<{ success: boolean }>(`${this.apiUrl}/profiles/verify-certification`, payload);
  }

  unlockCertificationReview(certificationId: number): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrl}/certification-request/${certificationId}/unlock`, {});
  }

  lockCertificationReview(certificationId: number, lock: boolean): Observable<{ success: boolean }> {
    const params = new HttpParams().set('lock', lock.toString());
    return this.http.post<{ success: boolean }>(`${this.apiUrl}/certification-request/${certificationId}/lock`, {}, { params });
  }

  // ── AUDIT LOGS ────────────────────────────────────────────────────────────

  getAuditLogs(): Observable<AdminLog[]> {
    return this.http.get<any[]>(`${this.apiUrl}/logs`);
  }

  // ── FUTURES FONCTIONNALITÉS ACCESSIBLES FACILEMENT ────────────────────────

  inviteAdmin(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/admins/invite`, { email });
  }

  getGeographicZones(): Observable<GeographicZoneWithParent[]> {
    return this.http.get<GeographicZoneWithParent[]>(`${this.apiUrl}/locations-flat`);
  }

  updateRegion(regionData: {id?:number,name:string,parentId?:number}): Observable<any> {
    return this.http.post(`${this.apiUrl}/region`, regionData);
  }

  deleteRegion(regionId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/regions/${regionId}`);
  }

  updateService(serviceData:{id?: number, name: string, description?:string}): Observable<any> {
    return this.http.post(`${this.apiUrl}/service`, serviceData);
  }

  deleteService(serviceId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/services/${serviceId}`);
  }

  updateLegalText(key: string, content: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/legal`, { key, content });
  }
}
