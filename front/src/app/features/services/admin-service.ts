import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { switchMap, tap, shareReplay } from 'rxjs/operators';
import { environment } from "../../../environments/environment";
import { WorkerFullProfile, WorkerPrivateAccount, WorkerProfileForAdmin } from "../models/user.model";
import { GeographicZoneWithParent } from "../models/filter.model";
import { AdminLog, CertificationRequest, Service } from "../models/common.model";

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = `${environment.apiBase}/admin`;

  private certificationRefresh$ = new BehaviorSubject<void>(undefined);
  private servicesRefresh$ = new BehaviorSubject<void>(undefined);
  private zonesRefresh$ = new BehaviorSubject<void>(undefined);

  public pendingCertificationRequests$: Observable<CertificationRequest[]> = this.certificationRefresh$.pipe(
    switchMap(() => this.http.get<CertificationRequest[]>(`${this.apiUrl}/certification-requests`)),
    shareReplay(1)
  );

  public services$: Observable<Service[]> = this.servicesRefresh$.pipe(
    switchMap(() => this.http.get<Service[]>(`${this.apiUrl}/services`)),
    shareReplay(1)
  );

  public zones$: Observable<GeographicZoneWithParent[]> = this.zonesRefresh$.pipe(
    switchMap(() => this.http.get<GeographicZoneWithParent[]>(`${this.apiUrl}/locations-flat`)),
    shareReplay(1)
  );

  constructor(private http: HttpClient) {}

  refreshCertifications() {
    this.certificationRefresh$.next();
  }

  refreshServices() {
    this.servicesRefresh$.next();
  }

  refreshZones() {
    this.zonesRefresh$.next();
  }

  getUsers(): Observable<WorkerProfileForAdmin[]> {
    return this.http.get<WorkerProfileForAdmin[]>(`${this.apiUrl}/users`);
  }

  getWorkerProfileForAdmin(workerId: string): Observable<WorkerPrivateAccount> {
    return this.http.get<WorkerPrivateAccount>(`${this.apiUrl}/workers/${workerId}`);
  }

  setLockedStatus(workerId: string, lock: boolean): Observable<WorkerFullProfile> {
    const params = new HttpParams().set('lock', lock.toString());
    return this.http.post<WorkerFullProfile>(`${this.apiUrl}/profiles/${workerId}/set-locked`, {}, { params });
  }

  updateWorkerStatus(workerId: string, statusPayload: { locked?: boolean; banned?: boolean; hidden?: boolean }): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/profiles/${workerId}/status`, statusPayload);
  }

  updateDaysCredit(workerId: string, newDaysValue: number, reason: string): Observable<{ success: boolean; newDaysValue: number }> {
    const payload = { workerId, newDaysValue, reason };
    return this.http.post<{ success: boolean; newDaysValue: number }>(`${this.apiUrl}/profiles/update-days`, payload);
  }

  getCertificationRequests(): Observable<CertificationRequest[]> {
    return this.http.get<CertificationRequest[]>(`${this.apiUrl}/certification-requests`);
  }

  verifyCertification(requestId: number, approved: boolean, comment: string): Observable<any> {
    const payload = { requestId, approved, comment };
    return this.http.post(`${this.apiUrl}/profiles/verify-certification`, payload).pipe(
      tap(() => this.refreshCertifications())
    );
  }

  unlockCertificationReview(certificationId: number): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrl}/certification-request/${certificationId}/unlock`, {});
  }

  lockCertificationReview(certificationId: number, lock: boolean): Observable<{ success: boolean }> {
    const params = new HttpParams().set('lock', lock.toString());
    return this.http.post<{ success: boolean }>(`${this.apiUrl}/certification-request/${certificationId}/lock`, {}, { params });
  }

  getAuditLogs(): Observable<AdminLog[]> {
    return this.http.get<any[]>(`${this.apiUrl}/logs`);
  }

  getGeographicZones(): Observable<GeographicZoneWithParent[]> {
    return this.http.get<GeographicZoneWithParent[]>(`${this.apiUrl}/locations-flat`);
  }

  updateRegion(regionData: { id?: number, name: string, parentId?: number }): Observable<any> {
    return this.http.post(`${this.apiUrl}/region`, regionData).pipe(
      tap(() => this.refreshZones())
    );
  }

  deleteRegion(regionId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/regions/${regionId}`).pipe(
      tap(() => this.refreshZones())
    );
  }

  updateService(serviceData: { id?: number, name: string, description?: string }): Observable<any> {
    return this.http.post(`${this.apiUrl}/service`, serviceData).pipe(
      tap(() => this.refreshServices())
    );
  }

  deleteService(serviceId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/services/${serviceId}`).pipe(
      tap(() => this.refreshServices())
    );
  }

  updateLegalText(key: string, content: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/legal`, { key, content });
  }
}
