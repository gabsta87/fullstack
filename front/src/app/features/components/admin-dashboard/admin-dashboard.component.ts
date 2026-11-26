import { Component, OnInit, OnDestroy, HostListener, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AlertController, IonicModule } from '@ionic/angular';
import { TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { AdminService } from '../../services/admin-service';
import { addIcons } from "ionicons";
import {
  addOutline, chevronBackOutline, chevronForwardOutline, closeOutline, pencilOutline, trashOutline, personAddOutline,
  shieldCheckmarkOutline, lockClosedOutline, lockOpenOutline, closeCircleOutline, alertCircleOutline
} from "ionicons/icons";
import {combineLatest, firstValueFrom, map, Observable, Subject} from "rxjs";
import { switchMap, tap } from "rxjs/operators";
import { AdminLog, CertificationRequest, Service } from "../../models/common.model";
import { GeographicZoneWithParent } from "../../models/filter.model";
import { WorkerPrivateProfile, AdminUser } from "../../models/user.model";
import { AuthService } from "../../services/auth.service";
import { UserRole } from "../../models/roles";
import { canManageAccount } from "../../utils/admin-rights";
import {Select} from "primeng/select";
import {AdminWorkerReport} from "../../models/items.model";
import {Router} from "@angular/router";
import {LanguageManagerService} from "../../services/language-manager.service";
import {profileIssueKeys} from "../../utils/error-messages";
import {HeaderComponent} from "../header/header.component";

@Component({
    selector: 'app-admin-dashboard',
    imports: [CommonModule, FormsModule, IonicModule, TableModule, InputTextModule, TagModule, Select,
        HeaderComponent],
    templateUrl: './admin-dashboard.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit, OnDestroy {
  /** Object URL of the private certification photo currently displayed (released on close). */
  private certificationObjectUrl: string | null = null;

  currentTab: 'users' | 'certifications' | 'reports' | 'services' | 'zones' | 'logs' | 'admins' = 'users';

  users$!: Observable<AdminUser[]>;
  pendingCertificationRequests$!: Observable<CertificationRequest[]>;
  reportsSummary$!: Observable<AdminWorkerReport[]>;
  services$!: Observable<Service[]>;
  zones$!: Observable<GeographicZoneWithParent[]>;
  auditLogs$!: Observable<AdminLog[]>;
  displayedAuditLogs$!: Observable<any[]>;
  flatZones$!: Observable<{ id: number; name: string; level: number; parentId: number | null }[]>;

  selectedReportWorker: AdminWorkerReport | null = null;
  totalReportsCount$!: Observable<number>;
  isReportsModalOpen = false;

  selectedWorkerDetails: WorkerPrivateProfile | null = null;
  isCertifModalOpen: boolean = false;
  currentCertifRequestId: number | null = null;
  adminComment: string = '';

  private submitCertification$ = new Subject<{ requestId: number; approved: boolean; comment: string }>();

  selectedRole: string | null = null;

  predefinedReasons: string[] = [
    "Advertiser identity does not match control photo",
    "Some profile photos do not match control photo",
    "Control code is unreadable",
    "Control code is incorrect",
    ""
  ];

  activePreviewUrl: string | null = null;
  previewImagesList: string[] = [];
  previewIndex: number = 0;
  certificationPreviewList: string[] = [];

  constructor(
    private adminService: AdminService,
    private alertCtrl: AlertController,
    private location: Location,
    private authService: AuthService,
    private router : Router,
    protected languageService : LanguageManagerService
  ) {
    addIcons({ addOutline, trashOutline, pencilOutline, chevronBackOutline, chevronForwardOutline, closeOutline,
      personAddOutline, shieldCheckmarkOutline, lockClosedOutline, lockOpenOutline, closeCircleOutline, alertCircleOutline
    });
  }

  /** Ban, lock and delete are only offered on accounts the current admin may act on (see canManageAccount). */
  canManage(user: AdminUser): boolean {
    return canManageAccount(this.authService.getUser(), user);
  }

  @HostListener('window:beforeunload')
  unloadHandler() {
    if (this.currentCertifRequestId) {
      this.adminService.unlockCertificationReview(this.currentCertifRequestId).subscribe();
    }
  }

  onPredefinedReasonSelected(event: any) {
    this.adminComment = event.detail.value;
  }

  get roleOptions() {
    const currentLang = this.languageService.currentLanguage$.value?.admin?.roles;
    return [
      { label: currentLang?.all, value: null },
      { label: currentLang?.admin, value: UserRole.ADMIN },
      { label: currentLang?.client, value: UserRole.CLIENT },
      { label: currentLang?.worker, value: UserRole.WORKER }
    ];
  }

  ngOnInit() {
    // Users come from the service's reactive stream
    this.users$ = this.adminService.users$;

    this.auditLogs$ = this.adminService.getAuditLogs().pipe(
      map(logs => logs.sort((a:AdminLog, b : AdminLog) =>
        new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
      )));

    this.displayedAuditLogs$ = combineLatest([this.auditLogs$, this.users$]).pipe(
      map(([logs, users]) => {
        return logs.map(log => {
          // Property linking the log to the admin
          const adminId = (log as any).adminId;

          const matchingUser = users.find(u => u.id === adminId);

          return {
            ...log,
            adminUser: matchingUser || null // Attach the AdminUser (or null when not found)
          };
        });
      })
    );

    this.pendingCertificationRequests$ = this.adminService.pendingCertificationRequests$.pipe(
      map(requests => (requests || []).sort((a: any, b: any) =>
        new Date(a.processedAt || 0).getTime() - new Date(b.processedAt || 0).getTime()
      )));

    this.services$ = this.adminService.services$;
    this.zones$ = this.adminService.zones$;

    this.flatZones$ = this.zones$.pipe(
      map(zones => {
        if (!zones) return [];
        const parents = zones.filter(z => !z.parentId);
        const result: { id: number; name: string; level: number; parentId: number | null }[] = [];

        parents.forEach(parent => {
          result.push({ id: parent.id, name: parent.name, level: 0, parentId: parent.parentId ?? null });
          const children = zones.filter(z => z.parentId === parent.id);
          children.forEach(child => {
            result.push({ id: child.id, name: child.name, level: 1, parentId: parent.id });
          });
        });

        return result;
      })
    );

    this.submitCertification$.pipe(
      switchMap(({ requestId, approved, comment }) =>
        this.adminService.verifyCertification(requestId, approved, comment).pipe(
          tap(() => this.closeCertificationModal())
        )
      )
    ).subscribe({
      error: (err) => {
        console.error('Certification processing failed', err);
        alert(this.languageService.msg('admin_request_error'));
      }
    });

    this.reportsSummary$ = this.adminService.reportsSummary$;

    this.totalReportsCount$ = this.reportsSummary$.pipe(
      map(list => (list || []).reduce((acc, curr) => acc + curr.reportCount, 0))
    );

    this.adminService.refreshReports();

    // Initial data load
    this.adminService.refreshUsers();
    this.adminService.refreshCertifications();
    this.adminService.refreshServices();
    this.adminService.refreshZones();
  }

  ngOnDestroy() {
    if (this.currentCertifRequestId) {
      this.adminService.unlockCertificationReview(this.currentCertifRequestId).subscribe();
    }
  }

  goBack() {
    this.location.back();
  }

  async setLocked(user: any, lockState: boolean) {
    try {
      await firstValueFrom(this.adminService.updateWorkerStatus(user.id, { locked: lockState }));
    } catch (err) {
      console.error('Lock change failed', err);
    }
  }

  async deleteUser(user:any){
    if (!confirm(this.languageService.msg('admin_delete_user_confirm', { name: user.username }))) return;
    try {
      await firstValueFrom(this.adminService.deleteUser(user.id));
    } catch (err) {
      console.error('User deletion failed', user.id, err);
    }
  }

  async openServiceModal() {
    const name = prompt(this.languageService.msg('admin_service_name_prompt'));
    if (!name || name.trim() === '') return;
    const description = prompt(this.languageService.msg('admin_service_desc_prompt')) || undefined;

    try {
      await firstValueFrom(this.adminService.updateService({
        name: name.trim(),
        description: description ? description.trim() : undefined
      }));
    } catch (err: any) {
      alert(this.languageService.msg('admin_service_create_error'));
    }
  }

  async editServiceModal(service: any) {
    const name = prompt(this.languageService.msg('admin_service_rename_prompt'), service.name);
    if (name === null) return;
    if (name.trim() === '') {
      alert(this.languageService.msg('admin_service_name_empty'));
      return;
    }

    const description = prompt(this.languageService.msg('admin_service_edit_desc_prompt'), service.description || '');
    if (description === null) return;

    try {
      await firstValueFrom(this.adminService.updateService({
        id: service.id,
        name: name.trim(),
        description: description.trim()
      }));
    } catch (err:any) {
      alert(this.languageService.msg('admin_service_update_error'));
    }
  }

  async deleteService(id: number) {
    if (confirm(this.languageService.msg('admin_service_delete_confirm'))) {
      try {
        await firstValueFrom(this.adminService.deleteService(id));
      } catch (err: any) {
        alert(this.languageService.msg('admin_service_delete_error'));
      }
    }
  }

  async openZoneModal(parentId: number | null = null) {
    const title = this.languageService.msg(parentId ? 'admin_zone_child_prompt' : 'admin_zone_parent_prompt');
    const name = prompt(title);
    if (!name || name.trim() === '') return;

    try {
      await firstValueFrom(this.adminService.updateRegion({
        name: name.trim(),
        ...(parentId !== null ? { parentId } : {})
      }));
    } catch (err: any) {
      alert(this.languageService.msg('admin_zone_create_error'));
    }
  }

  async editZoneName(zone: any) {
    const newName = prompt(this.languageService.msg('admin_zone_rename_prompt'), zone.name);
    if (!newName || newName.trim() === '' || newName === zone.name) return;

    try {
      await firstValueFrom(this.adminService.updateRegion({
        id: zone.id,
        name: newName.trim(),
        parentId: zone.parentId
      }));
    } catch (err: any) {
      alert(this.languageService.msg('admin_zone_update_error'));
    }
  }

  async deleteZone(id: number) {
    if (confirm(this.languageService.msg('admin_zone_delete_confirm'))) {
      try {
        await firstValueFrom(this.adminService.deleteRegion(id));
      } catch (err: any) {
        alert(this.languageService.msg(err?.status === 409 ? 'admin_zone_in_use' : 'admin_zone_delete_error'));
      }
    }
  }

  async openCertificationModal(request: CertificationRequest) {
    const tokenData = this.authService.getDecodedToken();
    const currentAdminId = tokenData?.id || tokenData?.userId;
    const isLockedByOther = request.underReview && request.lockedByAdminId && request.lockedByAdminId !== currentAdminId;

    if (isLockedByOther) {
      alert(this.languageService.msg('admin_request_locked'));
      return;
    }

    this.currentCertifRequestId = request.id;
    this.adminComment = '';

    try {
      await firstValueFrom(this.adminService.lockCertificationReview(request.id, true));

      const workerDetails = await firstValueFrom(
        this.adminService.getWorkerPrivateProfile(request.workerId)
      );

      const formattedCertifUrl = await this.adminService.loadCertificationPhoto(request.certificationPhotoUrl);
      this.certificationObjectUrl = formattedCertifUrl.startsWith('blob:') ? formattedCertifUrl : null;

      this.selectedWorkerDetails = {
        ...workerDetails,
        certificationRequest: workerDetails.certificationRequest
          ? { ...workerDetails.certificationRequest, certificationPhotoUrl: formattedCertifUrl }
          : workerDetails.certificationRequest,
        certificationRequestId: request.id,
        verificationCode: request.verificationCode,
        certificationPhotoUrl: formattedCertifUrl
      } as any;

      const galleryUrls = (workerDetails.photos || []).map(p => p.mainThumbUrl);
      this.certificationPreviewList = [formattedCertifUrl, ...galleryUrls];

      this.isCertifModalOpen = true;
    } catch (err) {
      console.error('Could not lock or load the request', err);
      alert(this.languageService.msg('admin_request_open_error'));
      this.currentCertifRequestId = null;
    }
  }

  async closeCertificationModal() {
    if (this.currentCertifRequestId) {
      try {
        await firstValueFrom(this.adminService.unlockCertificationReview(this.currentCertifRequestId));
      } catch (err) {
        console.error('Could not unlock the request', err);
      }
    }

    this.isCertifModalOpen = false;
    this.selectedWorkerDetails = null;
    if (this.certificationObjectUrl) {
      URL.revokeObjectURL(this.certificationObjectUrl);
      this.certificationObjectUrl = null;
    }
    this.currentCertifRequestId = null;
    this.adminComment = '';
  }

  submitCertificationReview(approved: boolean) {
    const requestId = this.currentCertifRequestId || (this.selectedWorkerDetails as any)?.certificationRequestId;

    if (!requestId) {
      alert(this.languageService.msg('admin_request_missing_id'));
      return;
    }

    const comment = this.adminComment.trim();

    if (!approved && !comment) {
      alert(this.languageService.msg('admin_comment_required'));
      return;
    }

    this.submitCertification$.next({ requestId, approved, comment });
  }

  openImagePreview(url: string, allImages: string[] = []) {
    this.activePreviewUrl = url;
    this.previewImagesList = allImages;
    this.previewIndex = allImages.indexOf(url);
  }

  closeImagePreview() {
    this.activePreviewUrl = null;
    this.previewImagesList = [];
    this.previewIndex = 0;
  }

  nextPreviewImage() {
    if (this.previewImagesList.length === 0) return;
    this.previewIndex = (this.previewIndex + 1) % this.previewImagesList.length;
    this.activePreviewUrl = this.previewImagesList[this.previewIndex];
  }

  prevPreviewImage() {
    if (this.previewImagesList.length === 0) return;
    this.previewIndex = (this.previewIndex - 1 + this.previewImagesList.length) % this.previewImagesList.length;
    this.activePreviewUrl = this.previewImagesList[this.previewIndex];
  }

  openReportsModal(workerReport: AdminWorkerReport) {
    this.selectedReportWorker = workerReport;
    this.isReportsModalOpen = true;
  }

  closeReportsModal() {
    this.selectedReportWorker = null;
    this.isReportsModalOpen = false;
    this.adminService.refreshReports(); // Refresh in case of a ban / lock
  }

  async dismissReport(reportId: number) {
    try {
      await firstValueFrom(this.adminService.dismissReport(reportId));
      this.adminService.refreshReports();
    } catch (err) {
      console.error('Could not dismiss the report', err);
      alert(this.languageService.msg('admin_report_dismiss_error'));
    }
  }

  async revokeCertification(user: any) {
    // 1. Ask for the reason
    const reason = prompt(this.languageService.msg('admin_revoke_prompt', { name: user.username }));


    if (!reason || reason.trim() === '') {
      alert(this.languageService.msg('admin_revoke_reason_required'));
      return;
    }

    try {
      // 3. Call the service with the id and the reason
      await firstValueFrom(this.adminService.revokeCertification(user.id, reason.trim()));
      this.adminService.refreshUsers();
      alert(this.languageService.msg('admin_revoke_success'));
    } catch (err) {
      console.error('Certification revocation failed', err);
      alert(this.languageService.msg('admin_revoke_error'));
    }
  }

  async banUser(worker: any, currentBannedState: boolean) {
    try {
      await firstValueFrom(this.adminService.updateWorkerStatus(worker.id, { banned: !currentBannedState }));
      this.adminService.refreshReports();
      this.adminService.refreshUsers();
    } catch (err) {
      console.error('Ban change failed', err);
    }
  }

  /** Translated reasons why this worker profile is not public (empty for other roles). */
  profileIssueLabels(user: AdminUser): string[] {
    return profileIssueKeys(user.profileIssues).map(key => this.languageService.msg(key));
  }

  viewProfile(user: any) {
    if (user.role === 'WORKER' || user.role === UserRole.WORKER) {
      this.router.navigate(['/profile'], {
        queryParams: { id: user.id },
        state: { from: this.router.url }
      });
    } else {
      alert(this.languageService.msg('admin_not_worker', { role: user.role }));
    }
  }

  async inviteAdminModal() {
    const alertRef = await this.alertCtrl.create({
      header: this.languageService.msg('admin_invite_title'),
      inputs: [
        { name: 'email', type: 'email', placeholder: this.languageService.msg('admin_invite_placeholder') }
      ],
      buttons: [
        { text: this.languageService.currentLanguage$.value?.common?.cancel || 'Cancel', role: 'cancel' },
        {
          text: this.languageService.msg('admin_invite_send'),
          handler: async (data) => {
            if (!data.email || data.email.trim() === '') {
              alert(this.languageService.msg('admin_invite_email_required'));
              return false;
            }
            try {
              await firstValueFrom(this.adminService.inviteAdmin(data.email.trim()));
              alert(this.languageService.msg('admin_invite_success'));
              this.adminService.refreshUsers();
              return true;
            } catch (err: any) {
              console.error('Admin invitation failed', err);
              alert(this.languageService.msg(err?.status === 409 ? 'email_taken' : 'admin_invite_error'));
              return false;
            }
          }
        }
      ]
    });
    await alertRef.present();
  }
}
