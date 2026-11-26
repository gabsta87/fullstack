import {Component, OnInit, ChangeDetectionStrategy} from '@angular/core';
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
  eyeOutline, mailOutline, closeCircle, addOutline, alertCircleOutline
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
import {siteLanguageTag} from "../../interceptors/jwt.interceptor";

@Component({
    selector: 'app-profile-management',
    imports: [CommonModule, FormsModule, IonicModule, HeaderComponent, AccountSettingsComponent, ZoneSelectorComponent, ServiceInfoComponent],
    templateUrl: './profile-management.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrls: ['./profile-management.component.scss']
})
export class ProfileManagementComponent implements OnInit {

  activeTab     : 'profile' | 'photos' | 'settings' | 'subscription' = 'profile';

  currentUser$! : Observable<WorkerPrivateProfile>;
  allServices!  : Service[];
  photos$!: Observable<(PhotoItem & { isMain: boolean })[]>;
  photos: (PhotoItem & { isMain: boolean })[] = [];
  videos$!: Observable<VideoItem[]>;
  allLocations! : GeographicZone[];
  childZoneId: number | undefined = undefined;
  parentZoneId: number | undefined = undefined;

  // Profile form
  lastServerState: WorkerProfileUpdate = {};
  profileForm: WorkerProfileUpdate = {};

  dragOverIndex: number | null = null;
  draggedIndex: number | null = null;
  isZoneActive: boolean = false;

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
      eyeOutline, eyeOffOutline, mailOutline, closeCircle, addOutline, alertCircleOutline
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
          this.profileLanguages = [...user.spokenLanguages];
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

    // 3. Photos derived from currentUser$
    this.photos$ = this.currentUser$.pipe(
      map(user => {
        const rawPhotos = user.photos || [];

        // Sort the photos by sortOrder before mapping them
        const sortedPhotos = [...rawPhotos].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));

        return sortedPhotos.map(photo => ({
          ...photo,
          isMain: photo.mainThumbUrl === user.mainThumbUrl,
          mainThumbUrl: photo.mainThumbUrl // Relative /media URL: served by nginx (proxy.conf.json in dev)
        }));
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
   * Color and label of the certification, valid for 8 months after it was granted (certifiedAt).
   * - Green (< 6 months): valid
   * - Yellow (6 to 7 months): renewal soon
   * - Orange (7 to 8 months): about to expire
   * - Red (> 8 months or not certified): expired / invalid
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
    const status = me.certificationRequest?.status;

    // 1. Waiting for the photo (or first request)
    if (status === CertificationStatus.PENDING_PHOTO || !status || status === CertificationStatus.NOT_CERTIFIED) {
      return {
        color: 'tertiary',
        label: t.certification?.title || 'Certification',
        buttonText: t.certification?.title || 'Certification',
        icon: 'camera-outline',
        isCert: false,
        hasNotification: false,
        isDisabled: false
      };
    }

    // 2. Correction requested by the admin OR rejected
    if (status === CertificationStatus.NEEDS_REVISION || status === CertificationStatus.REJECTED) {
      return {
        color: 'danger',
        label: me.adminCertificationFeedback
          ? this.languageService.msg('cert_rejected', { reason: me.adminCertificationFeedback })
          : this.languageService.msg('cert_revision_required'),
        buttonText: t.certification?.title || 'Certification',
        icon: 'alert-circle-outline',
        isCert: false,
        hasNotification: true,
        isDisabled: false
      };
    }

    // 3. Waiting for the admins -> LOCKED
    if (status === CertificationStatus.PENDING_APPROVAL) {
      return {
        color: 'medium',
        label: 'En attente de validation',
        buttonText: t.admin?.certif_tab?.badge_pending || 'En attente',
        icon: 'time-outline',
        isCert: false,
        hasNotification: false,
        isDisabled: true
      };
    }

    // 4. Certified
    if (status === CertificationStatus.CERTIFIED) {
      const certifiedDate = new Date(me.certifiedAt!).getTime();
      const currentTime = new Date().getTime();
      const diffInDays = Math.floor((currentTime - certifiedDate) / (1000 * 60 * 60 * 24));
      const diffInMonths = diffInDays / 30.44;

      if (diffInMonths > 8) {
        return { color: 'danger', label: this.languageService.msg('cert_expired'), buttonText: t.certification?.title || 'Certification', icon: 'camera-outline', isCert: false, hasNotification: false, isDisabled: false };
      } else if (diffInMonths >= 6) {
        return { color: 'warning', label: this.languageService.msg('cert_expiring'), buttonText: this.languageService.msg('cert_expiring_btn'), icon: 'shield-outline', isCert: true, hasNotification: false, isDisabled: false };
      } else {
        return { color: 'success', label: this.languageService.msg('cert_certified'), buttonText: this.languageService.msg('cert_certified_btn'), icon: 'shield-checkmark-outline', isCert: true, hasNotification: false, isDisabled: true };
      }
    }

    return {
      color: 'danger',
      label: 'Certification',
      buttonText: t.certification?.title || 'Certification',
      icon: 'shield-outline',
      isCert: false,
      hasNotification: false,
      isDisabled: false
    };
  }

  /**
   * Single entry point of the certification button:
   * opens the modal with the current user.
   */
  async openCertificationModal(me: WorkerPrivateProfile) {
    try {
      if (!me.certificationRequest?.verificationCode || me.certificationRequest?.status === CertificationStatus.NOT_CERTIFIED) {
        await this.workerAccountService.requestCertification();
      }

      await this.presentModal(me);

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
        verificationCode: me.certificationRequest?.verificationCode,
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

    this.profileLanguages.push(newItem);
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

  onZoneDragOver(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isZoneActive = true;
  }

  onZoneDragLeave() {
    this.isZoneActive = false;
  }

  async onFileSelected(event: any) {
    const files = event.target.files;
    if (files && files.length > 0) {
      await this.processAndUploadFiles(Array.from(files));
    }
    event.target.value = '';
  }

  async onZoneDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isZoneActive = false;

    if (event.dataTransfer && event.dataTransfer.files.length > 0) {
      await this.processAndUploadFiles(Array.from(event.dataTransfer.files));
    }
  }

  private async processAndUploadFiles(fileList: File[]) {
    const allowedImageTypes = ['image/jpeg', 'image/png', 'image/webp'];
    const allowedVideoTypes = ['video/mp4', 'video/quicktime', 'video/webm'];

    for (const file of fileList) {
      const isVideo = allowedVideoTypes.includes(file.type);
      const isImage = allowedImageTypes.includes(file.type);

      if (!isImage && !isVideo) {
        console.warn(`File rejected (unsupported format): ${file.name}`);
        return;
      }
    }

    await this.workerAccountService.uploadMedia(fileList);
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

  // DRAG AND DROP REORDER

  onDragStart(index: number) {
    this.draggedIndex = index;
  }

  onDragOver(event: DragEvent, index: number) {
    event.preventDefault();
    this.dragOverIndex = index;
  }

  async onDrop(targetIndex: number) {
    if (this.draggedIndex === null || this.draggedIndex === targetIndex) return;

    const movedPhoto = this.photos[this.draggedIndex];
    this.photos.splice(this.draggedIndex, 1);
    this.photos.splice(targetIndex, 0, movedPhoto);

    this.draggedIndex  = null;
    this.dragOverIndex = null;

    // Ids in the new visual order
    const orderedIds = this.photos.map(p => p.id);

    try {
      await this.workerAccountService.reorderPhotos(orderedIds);
    } catch (error) {
      console.error('Could not save the photo order', error);
    }
  }

  onDragEnd() {
    this.draggedIndex = null;
    this.dragOverIndex = null;
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
