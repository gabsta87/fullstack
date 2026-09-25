import {Component, OnInit} from '@angular/core';
import {CommonModule} from "@angular/common";
import {IonicModule} from "@ionic/angular";
import {FormsModule} from "@angular/forms";
import {HeaderComponent} from "../header/header.component";
import {filter, map, Observable} from "rxjs";
import {BODY_TYPE_LABELS, EYE_COLOR_LABELS, HAIR_COLOR_LABELS, PhotoItem, VideoItem} from "../../models/items.model";
import {ActivatedRoute} from "@angular/router";
import {WorkerPrivateAccount, WorkerProfileUpdate} from "../../models/user.model";
import {WorkerAccountService} from "../../services/worker-account.service";
import {tap} from "rxjs/operators";
import {addIcons} from "ionicons";
import {
  addCircleOutline,
  camera,
  cloudUploadOutline,
  move,
  star,
  starOutline,
  trashOutline,
  warningOutline
} from 'ionicons/icons';
import {AccountSettingsComponent} from "../account-settings/account-settings.component";
import {GeographicZone} from "../../models/filter.model";
import {ZoneSelectorComponent} from "../zone-selector/zone-selector.component";
import {environment} from "../../../../environments/environment";
import {Service} from "../../models/common.model";

@Component({
  selector: 'app-profile-management',
  imports: [CommonModule, FormsModule, IonicModule, HeaderComponent, AccountSettingsComponent, ZoneSelectorComponent],
  templateUrl: './profile-management.component.html',
  styleUrls: ['./profile-management.component.scss'],
  standalone: true
})
export class ProfileManagementComponent implements OnInit {

  activeTab     : 'profile' | 'photos' | 'settings' | 'subscription' = 'profile';

  currentUser$! : Observable<WorkerPrivateAccount>;
  allServices!  : Service[];
  photos!: (PhotoItem & { isMain: boolean })[];
  videos!: VideoItem[];
  allLocations! : GeographicZone[];
  childZoneId: number | undefined = undefined;
  parentZoneId: number | undefined = undefined;

  // Profile form
  lastServerState: WorkerProfileUpdate = {};
  profileForm: WorkerProfileUpdate = {};

  dragOverIndex: number | null = null;
  draggedIndex: number | null = null;
  isZoneActive: boolean = false;

  constructor(private accountService: WorkerAccountService, private route : ActivatedRoute) {
    addIcons({
      addCircleOutline, trashOutline, move, camera, warningOutline, star, starOutline, cloudUploadOutline
    });
  }

  async ngOnInit(): Promise<void> {
    // 1. Chargement des données statiques
    this.route.data.subscribe((data) => {
      this.allServices = data['services'] || [];
      this.allLocations = data['locations'] || [];
    });

    // 2. Flux unique pour l'UI & Synchronisation automatique du formulaire
    this.currentUser$ = this.accountService.listenToMyAccount().pipe(
      filter((user): user is WorkerPrivateAccount => user !== null),
      tap(user => {

        // 🎯 On mappe en calculant le booléen 'isMain'
        this.photos = (user.photos || []).map(photo => {
          const isCurrentMain = photo.mainThumbUrl === user.mainThumbUrl;

          return {
            ...photo,
            isMain: isCurrentMain,
            previewThumbUrl: photo.previewThumbUrl?.startsWith('http')
              ? photo.previewThumbUrl
              : `${environment.apiBase}${photo.previewThumbUrl}`,
            mainThumbUrl: photo.mainThumbUrl?.startsWith('http')
              ? photo.mainThumbUrl
              : `${environment.apiBase}${photo.mainThumbUrl}`
          };
        });

        this.childZoneId = user.geographicZone?.id;

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
          phone: user.phone,
          birthdate: cleanBirthdate
        };

        this.profileForm = { ...this.lastServerState };
      })
    );
  }

  isFieldMissing(field: string, me: any): boolean {
    if (!me) return true;
    switch (field) {
      case 'username':return !me.username || me.username.trim() === '';
      case 'email':
        const emailStr = typeof me.email === 'object' ? me.email?.value : me.email;
        return !emailStr || emailStr.trim() === '';
      case 'description':return !me.description || me.description.trim() === '';
      case 'geographicZoneId':return !me.geographicZone || !me.geographicZone.id || me.geographicZone.id == -1;
      case 'phone':return !me.phone || me.phone.trim() === '';
      case 'services':return !me.servicesId || me.servicesId.length === 0;
      case 'photos':return !this.photos || this.photos.length === 0;
      case 'birthday':return !me.birthdate;
      default:return false;
    }
  }

  getMissingFieldsList(me: any): string[] {
    const missing: string[] = [];
    if (!me) return missing;

    if (this.isFieldMissing('username', me)) missing.push("Nom d'utilisateur");
    if (this.isFieldMissing('email', me)) missing.push('Email');
    if (this.isFieldMissing('description', me)) missing.push('Description');
    if (this.isFieldMissing('geographicZoneId', me)) missing.push('Localisation');
    if (this.isFieldMissing('phone', me)) missing.push('Téléphone');
    if (this.isFieldMissing('birthday', me)) missing.push('Date de naissance');
    if (this.isFieldMissing('services', me)) missing.push('Au moins 1 service');
    if (this.isFieldMissing('photos', me)) missing.push('Au moins 1 photo');

    return missing;
  }

  // ── Value modification ─────────────────────────────────────────────────────────

  async toggleService(me: WorkerPrivateAccount, serviceId: number, event: any) {
    const isChecked = event.detail.checked;
    let updatedServices = [...(me.servicesId || [])];

    if (isChecked) {
      if (!updatedServices.includes(serviceId)) {
        updatedServices.push(serviceId);
      }
    } else {
      updatedServices = updatedServices.filter(s => s !== serviceId);
    }

    console.log("Services à envoyer au serveur:", updatedServices);
    await this.accountService.updateServices(updatedServices);
  }

  async toggleAvailable(currentAvailability: boolean) {
    await this.accountService.setAvailability(currentAvailability);
  }

  async updateProfileField(field: keyof WorkerProfileUpdate, value: any) {
    if (this.lastServerState[field] === value) {
      return;
    }

    console.log(`Envoi au serveur pour [${field}] :`, value);
    try {
      await this.accountService.updateProfile({ [field]: value });
      this.lastServerState[field] = value;
    } catch (error) {
      console.error(`Échec de la mise à jour pour ${field}`, error);
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
        console.warn(`Fichier refusé (format non supporté) : ${file.name}`);
        return;
      }
    }

    await this.accountService.uploadMedia(fileList);
  }

  async setMain(photo: PhotoItem) {
    await this.accountService.setMainPhoto(photo.id);
  }

  async deletePhoto(photo: PhotoItem) {
    await this.accountService.deletePhoto(photo.id);
  }

  async deleteVideo(video: VideoItem){
    await this.accountService.deleteVideo(video.id);
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

    const orderedIds = this.photos.map(p => p.id);
    try {
      await this.accountService.reorderPhotos(orderedIds);
    } catch (error) {
      console.error("Erreur lors de la sauvegarde de l'ordre des photos", error);
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
      await this.accountService.updateProfileData(payload);
      setSuccess('Vos paramètres de compte ont été mis à jour avec succès.');
    } catch (err: any) {
      setError(err?.error?.message || 'Une erreur est survenue lors de la mise à jour.');
    } finally {
      setLoading(false);
    }
  }

  handleSubscriptionClick() {
    console.log("Redirection vers la passerelle de paiement / Stripe / etc.");
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
          return 'Abonnement expiré';
        }

        if (hours > 48) {
          const days = Math.floor(hours / 24);
          return `${days} jour${days > 1 ? 's' : ''} restant${days > 1 ? 's' : ''}`;
        }

        return `${hours} heure${hours > 1 ? 's' : ''} restante${hours > 1 ? 's' : ''}`;
      })
    );
  }

  protected readonly BODY_TYPE_LABELS = BODY_TYPE_LABELS;
  protected readonly HAIR_COLOR_LABELS = HAIR_COLOR_LABELS;
  protected readonly EYE_COLOR_LABELS = EYE_COLOR_LABELS;
  protected readonly environment = environment;
  protected readonly isNaN = isNaN;
}
