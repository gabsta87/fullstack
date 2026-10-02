import {Component, OnInit} from '@angular/core';
import {CommonModule} from "@angular/common";
import {IonicModule} from "@ionic/angular";
import {FormsModule} from "@angular/forms";
import {HeaderComponent} from "../header/header.component";
import {filter, firstValueFrom, map, Observable} from "rxjs";
import {BODY_TYPE_LABELS, EYE_COLOR_LABELS, HAIR_COLOR_LABELS, PhotoItem, VideoItem} from "../../models/items.model";
import {ActivatedRoute} from "@angular/router";
import {WorkerPrivateAccount, WorkerProfileUpdate} from "../../models/user.model";
import {WorkerAccountService} from "../../services/worker-account.service";
import {tap} from "rxjs/operators";
import {addIcons} from "ionicons";
import {
  addCircleOutline, camera, cloudUploadOutline, move, star, starOutline, trashOutline,timeOutline,
  warningOutline, shieldCheckmarkOutline, shieldOutline, chevronDownCircleOutline, closeCircleOutline
} from 'ionicons/icons';
import {AccountSettingsComponent} from "../account-settings/account-settings.component";
import {GeographicZone} from "../../models/filter.model";
import {ZoneSelectorComponent} from "../zone-selector/zone-selector.component";
import {environment} from "../../../../environments/environment";
import {CertificationStatus, Service} from "../../models/common.model";
import {ModalController} from "@ionic/angular/standalone";
import {CertificationModalComponent} from "../certification-modal/certification-modal.component";

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

  constructor(private accountService: WorkerAccountService,
              private route : ActivatedRoute,
              private modalController: ModalController) {
    addIcons({
      addCircleOutline, trashOutline, move, camera, warningOutline, star, starOutline, cloudUploadOutline,
      shieldCheckmarkOutline, shieldOutline, chevronDownCircleOutline, closeCircleOutline, timeOutline
    });
  }

  async ngOnInit(): Promise<void> {
    // 1. Chargement des données statiques
    this.route.data.subscribe((data) => {
      this.allServices = data['services'] || [];
      this.allLocations = data['locations'] || [];
    });

    await firstValueFrom(this.accountService.getCurrentAccount());

    // 2. Flux unique pour l'UI & Synchronisation automatique du formulaire
    this.currentUser$ = this.accountService.listenToMyAccount().pipe(
      filter((user): user is WorkerPrivateAccount => user !== null),
      tap(user => {
        // Gestion des zones et du formulaire (reste inchangé)
        this.childZoneId = user.geographicZone?.id;

        this.photos = (user.photos || [])
          .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
          .map(photo => {
          const isCurrentMain = photo.mainThumbUrl === user.mainThumbUrl;
          return {
            ...photo,
            isMain: isCurrentMain,
            mainThumbUrl: photo.mainThumbUrl?.startsWith('http')
              ? photo.mainThumbUrl
              : `${environment.apiBase}${photo.mainThumbUrl}`
          };
        });

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

    // 3. Dérivation réactive des photos à partir de currentUser$
    this.photos$ = this.currentUser$.pipe(
      map(user => {
        const rawPhotos = user.photos || [];

        // 💡 On trie les photos selon leur sortOrder avant de les mapper
        const sortedPhotos = [...rawPhotos].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));

        return sortedPhotos.map(photo => ({
          ...photo,
          isMain: photo.mainThumbUrl === user.mainThumbUrl,
          mainThumbUrl: photo.mainThumbUrl?.startsWith('http')
            ? photo.mainThumbUrl
            : `${environment.apiBase}${photo.mainThumbUrl}`
        }));
      })
    );

    // 4. Dérivation réactive des vidéos
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

  // ── Gestion de la Certification ──────────────────────────────────────────

  /**
   * Calcule la couleur et le libellé de la certification selon un délai fixe de 8 mois
   * à partir de la date d'obtention (certifiedAt).
   * - Vert (< 6 mois) : Valide
   * - Jaune (6 à 7 mois) : Bientôt à renouveler
   * - Orange (7 à 8 mois) : Très proche de l'expiration
   * - Rouge (> 8 mois ou non certifié) : Expiré / Invalide
   */
  getCertificationBadge(me: WorkerPrivateAccount): {
      color: string;
      label: string;          // Tooltip complet
      buttonText: string;     // Texte court affiché sur le bouton
      icon: string;
    isCert: boolean;
    hasNotification: boolean;
    isDisabled: boolean;    // Pour bloquer le clic si besoin
  } {
    const status = me.certificationRequest?.status;
    console.log("status : "+me.certificationStatus);

    // 1. En attente de photo (ou premier choix)
    if (status === CertificationStatus.PENDING_PHOTO || !status || status === CertificationStatus.NOT_CERTIFIED) {
      return {
        color: 'tertiary',
        label: 'Cliquez pour demander la certification de votre profil',
        buttonText: 'Demander certification',
        icon: 'camera-outline',
        isCert: false,
        hasNotification: false,
        isDisabled: false
      };
    }

    // 2. Correction requise par l'admin OU Rejeté
    if (status === CertificationStatus.NEEDS_REVISION || status === CertificationStatus.REJECTED) {
      return {
        color: 'danger', // On met en rouge pour bien signifier qu'une action corrective est requise
        label: me.adminCertificationFeedback
          ? `Refusé / Correction demandée : ${me.adminCertificationFeedback}. Cliquez pour modifier.`
          : 'Votre photo a été refusée ou nécessite une correction. Cliquez pour refaire une demande.',
        buttonText: 'Refaire certification',
        icon: 'alert-circle-outline',
        isCert: false,
        hasNotification: true,
        isDisabled: false
      };
    }

    // 3. En attente de validation par les admins -> BLOQUÉ 🔒
    if (status === CertificationStatus.PENDING_APPROVAL) {
      return {
        color: 'medium',
        label: 'Demande en cours d\'examen par l\'administration',
        buttonText: 'En attente',
        icon: 'time-outline',
        isCert: false,
        hasNotification: false,
        isDisabled: true // 👈 Désactive le bouton
      };
    }

    // 4. Si certifié (avec gestion des délais d'expiration si tu veux garder tes couleurs)
    if (status === CertificationStatus.APPROVED) {
      const certifiedDate = new Date(me.certifiedAt!).getTime();
      const currentTime = new Date().getTime();
      const diffInDays = Math.floor((currentTime - certifiedDate) / (1000 * 60 * 60 * 24));
      const diffInMonths = diffInDays / 30.44;

      if (diffInMonths > 8) {
        return { color: 'danger', label: 'Certification expirée. Renouveler.', buttonText: 'Renouveler', icon: 'shield-alert-outline', isCert: false, hasNotification: false, isDisabled: false };
      } else if (diffInMonths >= 6) {
        return { color: 'warning', label: 'Votre certification expire bientôt.', buttonText: 'Certifié (Expire)', icon: 'shield-outline', isCert: true, hasNotification: false, isDisabled: false };
      } else {
        return { color: 'success', label: 'Profil certifié et à jour', buttonText: 'Certifié', icon: 'shield-checkmark-outline', isCert: true, hasNotification: false, isDisabled: false };
      }
    }

    // Fallback par défaut
    return {
      color: 'danger',
      label: 'Demander la certification',
      buttonText: 'Certifier',
      icon: 'shield-outline',
      isCert: false,
      hasNotification: false,
      isDisabled: false
    };
  }

  /**
   * Point d'entrée unique au clic sur le bouton de certification :
   * Ouvre directement la modale en lui passant l'utilisateur.
   */
  async openCertificationModal(me: WorkerPrivateAccount) {
    try {
      if (!me.verificationCode || me.certificationStatus === CertificationStatus.NOT_CERTIFIED || !me.certificationStatus) {
        await this.accountService.requestCertification();
      }

      await this.presentModal(me);

    } catch (err) {
      console.error("Erreur lors de la demande de certification", err);
    }
  }

  /**
   * Crée et présente la modale Ionic
   */
  async presentModal(me: WorkerPrivateAccount) {
    const modal = await this.modalController.create({
      component: CertificationModalComponent,
      componentProps: {
        verificationCode: me.verificationCode,
        adminMessage: me.adminCertificationFeedback
      }
    });

    await modal.present();

    const { data, role } = await modal.onDidDismiss();
    if (role === 'submitted' && data?.file) {
      const formData = new FormData();
      formData.append('file', data.file);

      try {
        // On délègue l'appel au service. Le service émettra la nouvelle version du profil.
        await this.accountService.uploadCertificationPhoto(formData);
      } catch (err) {
        console.error("Erreur lors de l'envoi de la photo de certification", err);
      }
    }
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

    // On récupère le tableau actuel depuis le flux ou une copie locale synchrone si besoin,
    // mais le plus propre avec un flux réactif est de mapper l'ordre des IDs directement :
    // Si tu as gardé un tableau 'photos' synchrone pour le drag & drop :
    const movedPhoto = this.photos[this.draggedIndex];
    this.photos.splice(this.draggedIndex, 1);
    this.photos.splice(targetIndex, 0, movedPhoto);

    this.draggedIndex  = null;
    this.dragOverIndex = null;

    // On extrait les IDs dans le nouvel ordre visuel
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
