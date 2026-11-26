import {Injectable, NgZone} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {BehaviorSubject, firstValueFrom, Observable, Subject} from 'rxjs';
import {auditTime, shareReplay, startWith, switchMap, tap} from 'rxjs/operators';
import {environment} from "../../../environments/environment";
import {AdminUser, WorkerPrivateProfile} from "../models/user.model";
import {GeographicZoneWithParent} from "../models/filter.model";
import {AdminLog, AdminSetting, CertificationRequest, Service} from "../models/common.model";
import {AuthService} from "./auth.service";
import {UserRole} from "../models/roles";
import {AdminWorkerReport} from "../models/items.model";

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = `${environment.apiBase}/admin`;

  /** Certification photos are private API resources (see backend AdminController). */
  static readonly PRIVATE_PHOTO_PREFIX = '/admin/certification-photos/';

  /**
   * Returns a displayable URL for a certification photo. Private photos are downloaded with the
   * admin token and exposed as an object URL (to release with URL.revokeObjectURL).
   */
  async loadCertificationPhoto(url: string | null | undefined): Promise<string> {
    if (!url) return '';
    if (!url.startsWith(AdminService.PRIVATE_PHOTO_PREFIX)) return url; // legacy public URL
    const blob = await firstValueFrom(this.http.get(`${environment.apiBase}${url}`, { responseType: 'blob' }));
    return URL.createObjectURL(blob);
  }
  private adminEventSource: EventSource | null = null;

  private usersRefresh$ = new Subject<void>();
  private certificationRefresh$ = new BehaviorSubject<void>(undefined);
  private servicesRefresh$ = new BehaviorSubject<void>(undefined);
  private zonesRefresh$ = new BehaviorSubject<void>(undefined);
  private reportsRefresh$ = new BehaviorSubject<void>(undefined);
  private auditLogsRefresh$ = new Subject<void>();
  private settingsRefresh$ = new Subject<void>();

  // The users list is heavy: one reload per second at most, whatever the number of events
  public users$: Observable<AdminUser[]> = this.usersRefresh$.pipe(
    auditTime(1000),
    startWith(undefined),
    switchMap(() => this.http.get<AdminUser[]>(`${this.apiUrl}/users`)),
    shareReplay(1)
  );

  public auditLogs$: Observable<AdminLog[]> = this.auditLogsRefresh$.pipe(
    auditTime(1000),
    startWith(undefined),
    switchMap(() => this.http.get<AdminLog[]>(`${this.apiUrl}/logs`)),
    shareReplay(1)
  );

  public settings$: Observable<AdminSetting[]> = this.settingsRefresh$.pipe(
    startWith(undefined),
    switchMap(() => this.http.get<AdminSetting[]>(`${this.apiUrl}/settings`)),
    shareReplay(1)
  );

  /** Saves the given values (all or nothing on the server) and returns the settings as stored. */
  async updateSettings(values: Record<string, number>): Promise<AdminSetting[]> {
    const saved = await firstValueFrom(this.http.put<AdminSetting[]>(`${this.apiUrl}/settings`, { values }));
    this.settingsRefresh$.next();
    return saved;
  }

  public pendingCertificationRequests$: Observable<CertificationRequest[]> = this.certificationRefresh$.pipe(
    switchMap(() => this.http.get<CertificationRequest[]>(`${this.apiUrl}/certification-requests`)),
    shareReplay(1)
  );

  public services$: Observable<Service[]> = this.servicesRefresh$.pipe(
    switchMap(() => this.http.get<Service[]>(`${environment.apiBase}/common/services`)),
    shareReplay(1)
  );

  public zones$: Observable<GeographicZoneWithParent[]> = this.zonesRefresh$.pipe(
    switchMap(() => this.http.get<GeographicZoneWithParent[]>(`${this.apiUrl}/regions-flat`)),
    shareReplay(1)
  );

  public reportsSummary$: Observable<AdminWorkerReport[]> = this.reportsRefresh$.pipe(
    switchMap(() => this.http.get<AdminWorkerReport[]>(`${this.apiUrl}/reports-summary`)),
    shareReplay(1)
  );

  constructor(private http: HttpClient, private zone: NgZone, private authService: AuthService) {
    // (Re)open the stream on every admin login, close it on logout
    this.authService.isAuthenticated$.subscribe(isAuth => {
      const role = this.authService.getUser()?.role;
      if (isAuth && (role === UserRole.ADMIN || role === UserRole.SUPER_ADMIN)) {
        this.initAdminSseListener();
      } else if (!isAuth) {
        this.closeAdminStream();
      }
    });
  }

  private initAdminSseListener() {
    if (this.adminEventSource) return;
    const token = localStorage.getItem('auth_token');
    if (!token) return;

    this.adminEventSource = new EventSource(`${this.apiUrl}/stream?token=${token}`);

    // 1. Certification events
    const certificationEvents = [
      'CERTIFICATION_UPDATED',
      'CERTIFICATION_LOCKED',
      'CERTIFICATION_UNLOCKED',
      'CERTIFICATION_PROCESSED'
    ];

    certificationEvents.forEach(eventName => {
      this.adminEventSource?.addEventListener(eventName, () => {
        this.zone.run(() => {
          this.refreshCertifications();
          this.refreshUsers(); // The users table shows the certification status
        });
      });
    });

    // Bans, locks, deletions, invitations made by another admin
    this.adminEventSource.addEventListener('USERS_UPDATED', () => {
      this.zone.run(() => this.refreshUsers());
    });

    // Any admin action adds a line to the audit log
    this.adminEventSource.addEventListener('AUDIT_LOG_UPDATED', () => {
      this.zone.run(() => this.auditLogsRefresh$.next());
    });

    // Settings changed by another admin
    this.adminEventSource.addEventListener('SETTINGS_UPDATED', () => {
      this.zone.run(() => this.settingsRefresh$.next());
    });

    this.adminEventSource.addEventListener('REPORT_UPDATED', () => {
      this.zone.run(() => this.refreshReports());
    });

    // 2. Service management events
    this.adminEventSource.addEventListener('SERVICES_UPDATED', () => {
      this.zone.run(() => this.refreshServices());
    });

    // 3. Region management events
    const regionEvents = ['REGIONS_UPDATED', 'ZONE_CREATED', 'ZONE_MODIFIED'];
    regionEvents.forEach(eventName => {
      this.adminEventSource?.addEventListener(eventName, () => {
        this.zone.run(() => this.refreshZones());
      });
    });


    this.adminEventSource.onerror = (err) => {
      console.warn('Admin SSE stream interrupted', err);
      if (this.adminEventSource?.readyState === EventSource.CLOSED) {
        this.closeAdminStream();
      }
    };
  }

  public refreshCertifications() {
    this.certificationRefresh$.next();
  }

  public refreshServices() {
    this.servicesRefresh$.next();
  }

  public refreshZones() {
    this.zonesRefresh$.next();
  }

  public refreshUsers() {
    this.usersRefresh$.next();
  }

  public refreshReports() {
    this.reportsRefresh$.next();
  }

  public closeAdminStream() {
    if (this.adminEventSource) {
      this.adminEventSource.close();
      this.adminEventSource = null;
    }
  }

  getUsers(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(`${this.apiUrl}/users`);
  }

  getCertificationRequests(): Observable<CertificationRequest[]> {
    return this.http.get<CertificationRequest[]>(`${this.apiUrl}/certification-requests`);
  }

  getGeographicZones(): Observable<GeographicZoneWithParent[]> {
    return this.http.get<GeographicZoneWithParent[]>(`${this.apiUrl}/regions-flat`);
  }

  getWorkerPrivateProfile(workerId: string): Observable<WorkerPrivateProfile> {
    return this.http.get<WorkerPrivateProfile>(`${this.apiUrl}/workers/${workerId}`);
  }

  updateWorkerStatus(workerId: string, statusPayload: { locked?: boolean; banned?: boolean; }): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/profiles/${workerId}/status`, statusPayload).pipe(
      tap(() => this.refreshUsers())
    );
  }

  deleteUser(userId:string):Observable<any>{
    return this.http.delete(`${this.apiUrl}/users/${userId}`).pipe(
      tap(() => this.refreshUsers())
    );
  }

  updateDaysCredit(workerId: string, newDaysValue: number, reason: string): Observable<any> {
    const payload = { workerId, newDaysValue, reason };
    return this.http.post(`${this.apiUrl}/profiles/update-days`, payload).pipe(
      tap(() => this.refreshUsers())
    );
  }

  verifyCertification(requestId: number, approved: boolean, comment: string): Observable<any> {
    const payload = { requestId, approved, comment };
    return this.http.post(`${this.apiUrl}/profiles/verify-certification`, payload).pipe(
      tap(() => {
        this.refreshCertifications();
        this.refreshUsers();
      })
    );
  }

  revokeCertification(workerId: string, reason: string): Observable<any> {
    const payload = { workerId, reason };
    return this.http.post(`${this.apiUrl}/profiles/revoke-certification`, payload).pipe(
      tap(() => {
        this.refreshCertifications();
      })
    )
  }

  unlockCertificationReview(certificationId: number): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrl}/certification-request/${certificationId}/unlock`, {});
  }

  lockCertificationReview(certificationId: number, lock: boolean): Observable<{ success: boolean }> {
    const params = new HttpParams().set('lock', lock.toString());
    return this.http.post<{ success: boolean }>(`${this.apiUrl}/certification-request/${certificationId}/lock`, {}, { params });
  }

  getAuditLogs(): Observable<AdminLog[]> {
    return this.auditLogs$;
  }

  // ── SERVICES ──────────────────────────────────────────

  updateService(servicePayload: { id?: number; name: string; description?: string }): Observable<any> {
    return this.http.post(`${this.apiUrl}/service`, servicePayload).pipe(
      tap(() => this.refreshServices())
    );
  }

  deleteService(serviceId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/services/${serviceId}`).pipe(
      tap(() => this.refreshServices())
    );
  }

  // ── ZONES / REGIONS ───────────────────────────────────

  updateRegion(regionPayload: { id?: number; name: string; parentId?: number | null }): Observable<any> {
    return this.http.post(`${this.apiUrl}/region`, regionPayload).pipe(
      tap(() => this.refreshZones())
    );
  }

  deleteRegion(regionId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/regions/${regionId}`).pipe(
      tap(() => this.refreshZones())
    );
  }

  // ── REPORTS ───────────────────────────────────

  dismissReport(reportId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/reports/${reportId}/dismiss`, {}).pipe(
      tap(() => this.refreshReports())
    );
  }

  // ── ADMINS ───────────────────────────────────

  inviteAdmin(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/invite`, { email }).pipe(
      tap(() => this.refreshUsers())
    );
  }
}
