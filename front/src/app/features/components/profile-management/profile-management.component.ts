import {Component, OnInit, ChangeDetectionStrategy, HostListener} from '@angular/core';
import {CdkDrag, CdkDragDrop, CdkDragPlaceholder, CdkDropList, moveItemInArray} from '@angular/cdk/drag-drop';
import {CommonModule} from "@angular/common";
import {IonicModule} from "@ionic/angular";
import {FormsModule} from "@angular/forms";
import {HeaderComponent} from "../header/header.component";
import {filter, firstValueFrom, map, Observable} from "rxjs";
import {BODY_TYPES_LIST, EYES_COLOR_LIST, HAIR_COLOR_LIST, PhotoItem, VideoItem} from "../../models/items.model";
import {ActivatedRoute} from "@angular/router";
import {WorkerPrivateProfile, WorkerProfileUpdate} from "../../models/user.model";
import {StorageUsage, WorkerAccountService} from "../../services/worker-account.service";
import {tap} from "rxjs/operators";
import {addIcons} from "ionicons";
import {
  addCircleOutline, camera, cloudUploadOutline, move, star, starOutline, trashOutline,timeOutline, cameraOutline,
  warningOutline, shieldCheckmarkOutline, shieldOutline, chevronDownCircleOutline, closeCircleOutline, eyeOffOutline,
  eyeOutline, mailOutline, closeCircle, addOutline, alertCircleOutline, personCircleOutline, downloadOutline
} from 'ionicons/icons';
import {AccountSettingsComponent} from "../account-settings/account-settings.component";
import {GeographicZone} from "../../models/filter.model";
import {ZoneSelectorComponent} from "../zone-selector/zone-selector.component";
import {ServiceInfoComponent} from "../service-info/service-info.component";
import {CertificationStatus, Service} from "../../models/common.model";
import {ModalController} from "@ionic/angular/standalone";
import {CertificationModalComponent} from "../certification-modal/certification-modal.component";
import {LanguageManagerService} from "../../services/language-manager.service";
import {AccountService} from "../../services/account-service";
import {sanitizePhone, sanitizePhoneTyping} from "../../utils/input-limits";
import {formatPhone} from "../../utils/phone";
import {settingsErrorKey} from "../../utils/error-messages";
import {certificationState} from "../../utils/certification-state";
import {sortByMastery} from "../../utils/languages";
import {siteLanguageTag} from "../../interceptors/jwt.interceptor";
import {formatFileSize} from "../../utils/file-size";
import {ALLOWED_UPLOAD_TYPES, isFileDrag, splitUploadable} from "../../utils/file-drag";

@Component({
    selector: 'app-profile-management',
    imports: [CommonModule, FormsModule, IonicModule, HeaderComponent, AccountSettingsComponent, ZoneSelectorComponent, ServiceInfoComponent,
      CdkDropList, CdkDrag, CdkDragPlaceholder],
    templateUrl: './profile-management.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrls: ['./profile-management.component.scss']
})
export class ProfileManagementComponent implements OnInit {

  activeTab     : 'profile' | 'photos' | 'settings' | 'subscription' = 'profile';

  currentUser$! : Observable<WorkerPrivateProfile>;
  allServices!  : Service[];
  photos: (PhotoItem & { isMain: boolean })[] = [];
  videos$!: Observable<VideoItem[]>;
  allLocations! : GeographicZone[];
  childZoneId: number | undefined = undefined;
  parentZoneId: number | undefined = undefined;

  // Profile form
  lastServerState: WorkerProfileUpdate = {};
  profileForm: WorkerProfileUpdate = {};

  /** Files are being dragged over the page: the drop zone turns green and takes them wherever they are dropped. */
  isFileDragOnPage = false;
  private fileDragDepth = 0;
  uploadMessage: string | null = null;
  readonly allowedUploadTypes = ALLOWED_UPLOAD_TYPES.join(', ');

  profileLanguages: { language: string; level: number }[] = [];
  newLangCode: string | null = null;
  newLangLevel: number | null = null;

  constructor(protected workerAccountService: WorkerAccountService,
              private accountService : AccountService,
              private route : ActivatedRoute,
              private modalController: ModalController,
              protected languageService: LanguageManagerService) {
    addIcons({
      addCircleOutline, trashOutline, move, camera, warningOutline, star, starOutline, cloudUploadOutline,
      shieldCheckmarkOutline, shieldOutline, chevronDownCircleOutline, closeCircleOutline, timeOutline,cameraOutline,
      eyeOutline, eyeOffOutline, mailOutline, closeCircle, addOutline, alertCircleOutline, personCircleOutline, downloadOutline
    });
  }

  async ngOnInit(): Promise<void> {
    // 1. Static data
    this.route.data.subscribe((data) => {
      this.allServices = data['services'] || [];
      this.allLocations = data['locations'] || [];
    });

    await firstValueFrom(this.workerAccountService.getCurrentAccount());
    void this.workerAccountService.refreshStorage();

    // 2. Single stream for the UI, keeps the form in sync
    this.currentUser$ = this.workerAccountService.listenToMyAccount().pipe(
      filter((user): user is WorkerPrivateProfile => user !== null),
      tap(user => {
        // Zones and form
        this.childZoneId = user.geographicZone?.id;

        this.photos = (user.photos || [])
          .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
          .map(photo => {
          const isCurrentMain = photo.mainThumbUrl === user.mainThumbUrl;
          return {
            ...photo,
            isMain: isCurrentMain,
            mainThumbUrl: photo.mainThumbUrl // Relative /media URL: served by nginx (proxy.conf.json in dev)
          };
        });

        if (user && user.spokenLanguages) {
          this.profileLanguages = sortByMastery(user.spokenLanguages);
        }

        if (this.childZoneId) {
          const parentZone = this.allLocations.find(parent =>
            parent.subZones?.some(sub => sub.id === this.childZoneId)
          );
          this.parentZoneId = parentZone ? parentZone.id : this.childZoneId;
        } else {
          this.parentZoneId = undefined;
        }

        let cleanBirthdate = '';
        if (user.birthdate) {
          const d = new Date(user.birthdate);
          if (!isNaN(d.getTime())) {
            cleanBirthdate = d.toISOString().split('T')[0];
          }
        }

        this.lastServerState = {
          username : user.username,
          bodyType: user.bodyType,
          eyeColor : user.eyeColor,
          hairColor : user.hairColor,
          geographicZoneId: user.geographicZone?.id,
          description: user.description,
          shortDescription : user.shortDescription,
          phone: user.phone,
          birthdate: cleanBirthdate
        };

        // The phone is stored without spaces but shown grouped ("+41 79 123 45 67").
        this.profileForm = { ...this.lastServerState, phone: formatPhone(user.phone) };
      })
    );

    // 4. Videos derived from currentUser$
    this.videos$ = this.currentUser$.pipe(
      map(user => user.videos || [])
    );
  }

  isFieldMissing(field: string, me: any): boolean {
    if (!me) return true;
    switch (field) {
      case 'username': return !me.username || me.username.trim() === '';
      case 'email':
        const emailStr = typeof me.email === 'object' ? me.email?.value : me.email;
        return !emailStr || emailStr.trim() === '';
      case 'description': return !me.description || me.description.trim() === '';
      case 'geographicZoneId': return !me.geographicZone || !me.geographicZone.id || me.geographicZone.id == -1;
      case 'phone': return !me.phone || me.phone.trim() === '';
      case 'services': return !me.servicesId || me.servicesId.length === 0;
      case 'photos': return !me.photos || me.photos.length === 0;
      case 'birthday': return !me.birthdate;
      default: return false;
    }
  }

  getMissingFieldsList(me: any): string[] {
    const missing: string[] = [];
    if (!me) return missing;

    const checks: [string, string][] = [
      ['username', 'missing_username'], ['email', 'missing_email'], ['description', 'missing_description'],
      ['geographicZoneId', 'missing_location'], ['phone', 'missing_phone'], ['birthday', 'missing_birthdate'],
      ['services', 'missing_service'], ['photos', 'missing_photo'],
    ];
    for (const [field, key] of checks) {
      if (this.isFieldMissing(field, me)) missing.push(this.languageService.msg(key));
    }

    return missing;
  }

  // ── Certification ────────────────────────────────────────────────────────

  /**
   * Color and label of the certification button. The end and renewal dates come from the server
   * (fixed at approval from the admin settings), see certificationState.
   */
  getCertificationBadge(me: WorkerPrivateProfile, t: any): {
    color: string;
    label: string;
    buttonText: string;
    icon: string;
    isCert: boolean;
    hasNotification: boolean;
    isDisabled: boolean;
  } {
    const title = t.certification?.title || 'Certification';
    const date = (value?: string | null) => value
      ? new Date(value).toLocaleDateString(this.languageService.currentCode.toLowerCase(), { day: 'numeric', month: 'long', year: 'numeric' })
      : '';
    const msg = (key: string, params: Record<string, string> = {}) => this.languageService.msg(key, params);

    switch (certificationState(me)) {
      case 'valid':
        return { color: 'success', label: msg('cert_valid_until', { date: date(me.certificationExpiresAt) }),
          buttonText: msg('cert_certified_btn'), icon: 'shield-checkmark-outline', isCert: true, hasNotification: false, isDisabled: true };
      case 'renewal_open':
        return { color: 'warning', label: msg('cert_renew_open', { date: date(me.certificationExpiresAt) }),
          buttonText: msg('cert_renew_btn'), icon: 'shield-outline', isCert: true, hasNotification: true, isDisabled: false };
      case 'renewal_pending':
        return { color: 'success', label: msg('cert_renewal_pending'),
          buttonText: msg('cert_certified_btn'), icon: 'time-outline', isCert: true, hasNotification: false, isDisabled: true };
      case 'pending_approval':
        return { color: 'medium', label: msg('cert_pending'),
          buttonText: t.admin?.certif_tab?.badge_pending || msg('cert_pending'), icon: 'time-outline', isCert: false, hasNotification: false, isDisabled: true };
      case 'revision':
        return { color: 'danger',
          label: me.adminCertificationFeedback
            ? msg('cert_rejected', { reason: me.adminCertificationFeedback })
            : msg('cert_revision_required'),
          buttonText: title, icon: 'alert-circle-outline', isCert: false, hasNotification: true, isDisabled: false };
      case 'expired':
        return { color: 'danger', label: msg('cert_expired'),
          buttonText: title, icon: 'camera-outline', isCert: false, hasNotification: false, isDisabled: false };
      default: // none, pending_photo
        return { color: 'tertiary', label: title, buttonText: title, icon: 'camera-outline', isCert: false, hasNotification: false, isDisabled: false };
    }
  }

  /**
   * Single entry point of the certification button:
   * opens the modal with the current user.
   */
  async openCertificationModal(me: WorkerPrivateProfile) {
    try {
      const status = me.certificationRequest?.status;
      // A renewal (status CERTIFIED) or a first request starts with a new verification code
      let current = me;
      if (!me.certificationRequest?.verificationCode || status === CertificationStatus.NOT_CERTIFIED || status === CertificationStatus.CERTIFIED) {
        // The modal shows the code just created: the profile returned by the request, not the one before it
        current = await this.workerAccountService.requestCertification();
      }

      await this.presentModal(current);

    } catch (err) {
      console.error('Certification request failed', err);
    }
  }

  /**
   * Creates and presents the Ionic modal.
   */
  async presentModal(me: WorkerPrivateProfile) {
    const modal = await this.modalController.create({
      component: CertificationModalComponent,
      componentProps: {
        verificationCode: me.certificationRequest?.verificationCode ?? me.verificationCode,
        adminMessage: me.adminCertificationFeedback
      }
    });

    await modal.present();

    const { data, role } = await modal.onDidDismiss();
    if (role === 'submitted' && data?.file) {
      const formData = new FormData();
      formData.append('file', data.file);

      try {
        // The service emits the updated profile
        await this.workerAccountService.uploadCertificationPhoto(formData);
      } catch (err) {
        console.error('Certification photo upload failed', err);
      }
    }
  }

  // Languages

  isLanguageAlreadyAdded(code: string): boolean {
    return this.profileLanguages.some(l => l.language === code);
  }

  async addLanguage() {
    if (!this.newLangCode || !this.newLangLevel) return;

    const newItem = {
      language: this.newLangCode,
      level: this.newLangLevel
    };

    this.profileLanguages = sortByMastery([...this.profileLanguages, newItem]);
    this.newLangCode = null;
    this.newLangLevel = null;

    await this.saveLanguages();
  }

// Remove a language
  async removeLanguage(index: number) {
    this.profileLanguages.splice(index, 1);
    await this.saveLanguages();
  }

// Appel Backend
  private async saveLanguages() {
    await this.workerAccountService.updateSpokenLanguages(this.profileLanguages);
  }

  // ── Value modification ─────────────────────────────────────────────────────────

  async toggleService(me: WorkerPrivateProfile, serviceId: number, event: any) {
    const isChecked = event.detail.checked;
    let updatedServices = [...(me.servicesId || [])];

    if (isChecked) {
      if (!updatedServices.includes(serviceId)) {
        updatedServices.push(serviceId);
      }
    } else {
      updatedServices = updatedServices.filter(s => s !== serviceId);
    }

    await this.workerAccountService.updateServices(updatedServices);
  }

  async toggleAvailable(currentAvailability: boolean) {
    await this.workerAccountService.setAvailability(currentAvailability);
  }

  async toggleVisible(currentVisibility: boolean){
    await this.workerAccountService.setVisibility(currentVisibility);
  }

  /**
   * Phone: digits and spaces, with an optional leading "+".
   * The field is corrected while typing (ionInput); on leaving the field (ionChange)
   * the number is saved without spaces and shown grouped again (onPhoneChange).
   */
  async onPhoneInput(event: CustomEvent): Promise<void> {
    const input = event.target as HTMLIonInputElement;
    const raw = String((event as any).detail?.value ?? '');
    const cleaned = sanitizePhoneTyping(raw);
    this.profileForm.phone = cleaned;
    if (cleaned !== raw) {
      // When the cleaned value did not change since the previous keystroke,
      // ion-input does not re-render: fix the native <input> directly.
      input.value = cleaned;
      const native = await input.getInputElement();
      native.value = cleaned;
    }
  }

  /** Message key explaining why a video could not be processed. */
  videoFailureKey(video: VideoItem): string {
    switch (video.failureReason) {
      case 'VIDEO_TOO_LONG': return 'video_too_long';
      case 'VIDEO_UNREADABLE': return 'video_unreadable';
      default: return 'video_processing_failed';
    }
  }

  async onPhoneChange(value: string | null | undefined): Promise<void> {
    const phone = sanitizePhone(value ?? '');
    await this.updateProfileField('phone', phone);
    this.profileForm.phone = formatPhone(this.lastServerState.phone);
  }

  async updateProfileField(field: keyof WorkerProfileUpdate, value: any) {
    if (this.lastServerState[field] === value) {
      return;
    }

    try {
      await this.workerAccountService.updateProfile({ [field]: value });
      this.lastServerState[field] = value;
    } catch (error) {
      console.error(`Update failed for ${field}`, error);
      (this.profileForm as any)[field] = this.lastServerState[field];
    }
  }

  // ── Photos ────────────────────────────────────────────────────────────────

  // Files dragged from the computer: the whole page accepts them (switching to the media tab)

  @HostListener('window:dragenter', ['$event'])
  onWindowDragEnter(event: DragEvent) {
    if (!isFileDrag(event)) return;
    event.preventDefault();
    this.fileDragDepth++;
    this.isFileDragOnPage = true;
    this.activeTab = 'photos';
  }

  @HostListener('window:dragover', ['$event'])
  onWindowDragOver(event: DragEvent) {
    if (!isFileDrag(event)) return;
    event.preventDefault(); // otherwise the browser opens the file
    if (event.dataTransfer) event.dataTransfer.dropEffect = 'copy';
  }

  @HostListener('window:dragleave', ['$event'])
  onWindowDragLeave(event: DragEvent) {
    if (!isFileDrag(event)) return;
    this.fileDragDepth = Math.max(0, this.fileDragDepth - 1);
    if (this.fileDragDepth === 0) this.isFileDragOnPage = false;
  }

  @HostListener('window:drop', ['$event'])
  async onWindowDrop(event: DragEvent) {
    if (!isFileDrag(event)) return;
    event.preventDefault();
    this.fileDragDepth = 0;
    this.isFileDragOnPage = false;
    await this.processAndUploadFiles(Array.from(event.dataTransfer?.files ?? []));
  }

  async onFileSelected(event: any) {
    const files = event.target.files;
    if (files && files.length > 0) {
      await this.processAndUploadFiles(Array.from(files));
    }
    event.target.value = '';
  }

  private async processAndUploadFiles(fileList: File[]) {
    const { accepted, refused } = splitUploadable(fileList);
    this.uploadMessage = refused.length
      ? this.languageService.msg('media_unsupported', { names: refused.map(f => f.name).join(', ') })
      : null;
    if (accepted.length) await this.workerAccountService.uploadMedia(accepted);
  }

  fileSizeLabel(bytes: number | null | undefined, t: any): string {
    return formatFileSize(bytes, siteLanguageTag(), {
      kb: t.profile_management?.media?.unit_kb || 'KB', mb: t.profile_management?.media?.unit_mb || 'MB' });
  }

  async setMain(photo: PhotoItem) {
    await this.workerAccountService.setMainPhoto(photo.id);
  }

  async deletePhoto(photo: PhotoItem) {
    await this.workerAccountService.deletePhoto(photo.id);
  }

  async deleteVideo(video: VideoItem){
    await this.workerAccountService.deleteVideo(video.id);
  }

  // Photos moved inside the page (CDK drag and drop: mouse and touch)

  /** Dropped on the profile photo slot: becomes the profile photo and stays in the gallery. */
  async onAvatarDrop(event: CdkDragDrop<unknown>) {
    const photo = event.item.data as PhotoItem & { isMain: boolean };
    if (!photo || photo.isMain) return;
    try {
      await this.setMain(photo);
    } catch (error) {
      console.error('Could not set the profile photo', error);
    }
  }

  /** Reordered inside the gallery: the new order is saved, and restored if the server refuses it. */
  async onGalleryDrop(event: CdkDragDrop<(PhotoItem & { isMain: boolean })[]>) {
    if (event.previousContainer !== event.container || event.previousIndex === event.currentIndex) return;
    const before = [...this.photos];
    moveItemInArray(this.photos, event.previousIndex, event.currentIndex);
    try {
      await this.workerAccountService.reorderPhotos(this.photos.map(p => p.id));
    } catch (error) {
      console.error('Could not save the photo order', error);
      this.photos = before;
    }
  }

  // ── Settings ──────────────────────────────────────────────────────────────

  async handleSettingsSave(event: any) {
    const { payload, setLoading, setSuccess, setError } = event;

    try {
      await this.accountService.updateSettings(payload);
      setSuccess(this.languageService.msg('settings_saved'));
    } catch (err: any) {
      setError(this.languageService.msg(settingsErrorKey(err)));
    } finally {
      setLoading(false);
    }
  }

  handleSubscriptionClick() {
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  get subscriptionHoursLeft$(): Observable<number> {
    return this.currentUser$.pipe(
      map(profile => {
        if (!profile || !profile.expirationDate) {
          return 0;
        }

        const expiryTime = new Date(profile.expirationDate).getTime();
        const currentTime = new Date().getTime();
        const diffInMs = expiryTime - currentTime;
        const hoursLeft = Math.floor(diffInMs / (1000 * 60 * 60));

        return hoursLeft > 0 ? hoursLeft : 0;
      })
    );
  }

  get subscriptionColor$(): Observable<string> {
    return this.subscriptionHoursLeft$.pipe(
      map(hours => {
        if (hours <= 24) return 'danger';
        if (hours <= 168) return 'warning';
        return 'success';
      })
    );
  }

  get subscriptionLabel$(): Observable<string> {
    return this.subscriptionHoursLeft$.pipe(
      map(hours => {
        if (hours <= 0) {
          return this.languageService.msg('subscription_expired');
        }

        if (hours > 48) {
          const days = Math.floor(hours / 24);
          return this.languageService.msg(days > 1 ? 'days_left_other' : 'days_left_one', { n: days });
        }

        return this.languageService.msg(hours > 1 ? 'hours_left_other' : 'hours_left_one', { n: hours });
      })
    );
  }

  /** "Storage used: 12.3 MB of 50 MB (25 %)" and the bar value (0..1). */
  storageLabel(usage: StorageUsage): string {
    const format = new Intl.NumberFormat(siteLanguageTag(), { maximumFractionDigits: 1 });
    const mb = (bytes: number) => format.format(bytes / (1024 * 1024));
    return this.languageService.msg('storage_used', {
      used: mb(usage.usedBytes), total: mb(usage.maxBytes), percent: Math.round(this.storageRatio(usage) * 100)
    });
  }

  storageRatio(usage: StorageUsage): number {
    return usage.maxBytes > 0 ? Math.min(1, usage.usedBytes / usage.maxBytes) : 0;
  }

  protected readonly isNaN = isNaN;
  protected readonly BODY_TYPES_LIST = BODY_TYPES_LIST;
  protected readonly HAIR_COLOR_LIST = HAIR_COLOR_LIST;
  protected readonly EYES_COLOR_LIST = EYES_COLOR_LIST;
}

/** Keeps digits only, plus an optional leading "+". */
