import {Component, OnInit} from '@angular/core';
import {CommonModule} from "@angular/common";
import {IonicModule, ItemReorderEventDetail} from "@ionic/angular";
import {FormsModule} from "@angular/forms";
import {HeaderComponent} from "../header/header.component";
import {map, Observable} from "rxjs";
import {BODY_TYPE_LABELS, PhotoItem} from "../../models/items.model";
import {ActivatedRoute} from "@angular/router";
import {WorkerFullProfile, WorkerPrivateAccount, WorkerProfileUpdate} from "../../models/user.model";
import {WorkerAccountService} from "../../services/worker-account.service";
import {tap} from "rxjs/operators";
import {addIcons} from "ionicons";
import {addCircleOutline, camera, move, trashOutline, warningOutline} from 'ionicons/icons';
import {AccountSettingsComponent} from "../account-settings/account-settings.component";
import {GeographicZone} from "../../models/filter.model";
import {ZoneSelectorComponent} from "../zone-selector/zone-selector.component";
import {environment} from "../../../../environments/environment";

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
  allServices!  : string[];
  photos!       : PhotoItem[];
  allLocations! : GeographicZone[];

  // Profile form
  profileForm: WorkerProfileUpdate = {};

  dragOverIndex: number | null = null;
  draggedIndex: number | null = null;

  // Settings
  settingsForm = { username: '', email: '', password: '' };
  settingsPassword2 = '';
  settingsSaved = false;
  settingsError = '';
  savingSettings = false;

  constructor(private accountService: WorkerAccountService, private route : ActivatedRoute) {
    addIcons({
      addCircleOutline,trashOutline,move, camera, warningOutline
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
      tap(user => {
        if (user) {

          this.photos = (user.photos || []).map(photo => {
            return {
              ...photo,
              previewThumbUrl: photo.previewThumbUrl?.startsWith('http')
                ? photo.previewThumbUrl
                : `${environment.apiBase}${photo.previewThumbUrl}`,
              mainThumbUrl: photo.mainThumbUrl?.startsWith('http')
                ? photo.mainThumbUrl
                : `${environment.apiBase}${photo.mainThumbUrl}`
            };
          });

          // SÉCURISATION DE LA DATE POUR LE CALENDRIER HTML5
          let cleanBirthdate = '';
          if (user.birthdate) {
            const d = new Date(user.birthdate);
            if (!isNaN(d.getTime())) {
              cleanBirthdate = d.toISOString().split('T')[0];
            }
          }

          this.profileForm = {
            bodyType: user.bodyType,
            geographicZoneId: user.geographicZone?.id,
            description: user.description,
            phone: user.phone,
            birthdate: cleanBirthdate
          };
        }
      })
    );
  }

  isFieldMissing(field: string, me: any): boolean {
    if (!me) return false;
    switch (field) {
      case 'username': return !me.username || me.username.trim() === '';
      case 'email': return !me.email || me.email.trim() === '';
      case 'description': return !me.description || me.description.trim() === '';
      case 'geographicZoneId': return !me.geographicZoneId;
      case 'phone': return !me.phone || me.phone.trim() === '';
      case 'services': return !me.services || me.services.length === 0;
      case 'photos': return !this.photos || this.photos.length === 0;
      case 'birthday': return !me.birthday;
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

  setTab(tab: 'profile' | 'photos' | 'settings' | 'subscription') {
    this.activeTab = tab;
    this.settingsSaved = false;
    this.settingsError = '';
  }

  // ── Value modification ─────────────────────────────────────────────────────────

  toggleService(me: WorkerFullProfile, service: string) {
    // 1. Calculer la nouvelle liste localement
    let updatedServices = [...me.services];
    if (updatedServices.includes(service)) {
      updatedServices = updatedServices.filter(s => s !== service);
    } else {
      updatedServices.push(service);
    }
    console.log("Services à envoyer au serveur:", updatedServices);

    // 2. Envoyer au serveur
    // this.accountService.updateServices(updatedServices);
    this.accountService.updateServices(updatedServices);
  }

  async toggleAvailable(currentAvailability: boolean) {
    console.log("toggling availability to", currentAvailability);
    try {
      await this.accountService.setAvailability(currentAvailability);
    } catch (error) {
      console.error("Erreur lors du changement de disponibilité", error);
    }
  }

  async updateProfileField(field: keyof WorkerProfileUpdate, value: any) {
    if (this.profileForm[field] === value) {
      return;
    }

    console.log(`Mise à jour du champ [${field}] demandé avec :`, value);
    try {
      // On envoie la modification au serveur.
      // Dès que la promesse résout, le service pousse la nouvelle valeur dans le Subject,
      // ce qui déclenche le 'tap' du ngOnInit ci-dessus et met à jour ton UI tout seul !
      await this.accountService.updateProfile({ [field]: value });
    } catch (error) {
      console.error(`Échec de la mise à jour réseau pour le champ ${field}`, error);
      // L'UI ne bouge pas et reste synchronisée sur l'ancienne valeur valide en cas de coupure.
    }
  }

  // ── Photos ────────────────────────────────────────────────────────────────

  async onFileSelected(event: any) {
    const file: File = event.target.files[0];
    if (!file) return;

    await this.accountService.uploadPhoto(file);
  }

  // 2. Gérer le réordonnancement (Drag & Drop)
  async doReorder(event: CustomEvent<ItemReorderEventDetail>) {
    // Le tableau local est mis à jour automatiquement par Ionic
    this.photos = event.detail.complete(this.photos);

    // Envoyer le nouvel ordre au backend
    const orderedIds = this.photos.map(p => p.id);
    await this.accountService.reorderPhotos(orderedIds);
  }

  // async addImageToGallery(event: any) {
  //
  //   let data = {} as any;
  //   data.file = event.target.files[0];
  //   data.name = data.file.name;
  //   data.collectionId = this.imagesCollections[this.selectedGallery].id;
  //
  //   this.isLoadingGallery.next(true);
  //   await this.storage.addImageToGallery(data);
  //   this.isLoadingGallery.next(false);
  // }

  setMain(photo: PhotoItem) {
    this.accountService.setMainPhoto(photo.id);
  }

  deletePhoto(photo: PhotoItem) {
    this.accountService.deletePhoto(photo.id);
  }

  // DRAG AND DROP REORDER

  onDragStart(index: number) {
    this.draggedIndex = index;
  }

  onDragOver(event: DragEvent, index: number) {
    event.preventDefault(); // Indispensable pour autoriser le "drop"
    this.dragOverIndex = index;
  }

  async onDrop(targetIndex: number) {
    if (this.draggedIndex === null || this.draggedIndex === targetIndex) return;

    // 1. On réorganise le tableau localement pour un rendu visuel instantané
    const movedPhoto = this.photos[this.draggedIndex];
    this.photos.splice(this.draggedIndex, 1);       // Supprime de l'ancienne position
    this.photos.splice(targetIndex, 0, movedPhoto); // Insère à la nouvelle position

    // Reset des index de drag
    this.draggedIndex  = null;
    this.dragOverIndex = null;

    // 2. On extrait les IDs ordonnés et on synchronise avec le serveur
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

  saveSettings() {
    this.settingsError = '';
    this.settingsSaved = false;

    if (this.settingsForm.password && this.settingsForm.password !== this.settingsPassword2) {
      this.settingsError = 'Les mots de passe ne correspondent pas.';
      return;
    }

    const payload: any = {};
    if (this.settingsForm.username) payload.username = this.settingsForm.username;
    if (this.settingsForm.email)    payload.email    = this.settingsForm.email;
    if (this.settingsForm.password) payload.password = this.settingsForm.password;

    this.savingSettings = true;
    // this.accountService.updateSettings(payload);
  }

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
    // Votre logique de modal ou de redirection vers le renouvellement
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  get subscriptionHoursLeft$(): Observable<number> {
    return this.currentUser$.pipe(
      map(profile => {
        // Si le profil n'existe pas ou s'il n'a pas de date d'expiration
        if (!profile || !profile.expirationDate) {
          return 0;
        }

        const expiryTime = new Date(profile.expirationDate).getTime();
        const currentTime = new Date().getTime();
        const diffInMs = expiryTime - currentTime;

        // Convertir les millisecondes en heures et arrondir à l'inférieur
        const hoursLeft = Math.floor(diffInMs / (1000 * 60 * 60));

        // Retourner 0 si l'abonnement est déjà expiré (valeur négative)
        return hoursLeft > 0 ? hoursLeft : 0;
      })
    );
  }

  get subscriptionColor$(): Observable<string> {
    return this.subscriptionHoursLeft$.pipe(
      map(hours => {
        if (hours <= 24) return 'danger';
        if (hours <= 168) return 'warning'; // 168 heures = 7 jours
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

        // S'il reste plus de 48 heures, on affiche en Jours
        if (hours > 48) {
          const days = Math.floor(hours / 24);
          return `${days} jour${days > 1 ? 's' : ''} restant${days > 1 ? 's' : ''}`;
        }

        // S'il reste moins de 48 heures, on affiche précisément en Heures
        return `${hours} heure${hours > 1 ? 's' : ''} restante${hours > 1 ? 's' : ''}`;
      })
    );
  }

  protected readonly BODY_TYPE_LABELS = BODY_TYPE_LABELS;
  // protected readonly EYE_COLOR_LABELS = EYE_COLOR_LABELS;
  // protected readonly HAIR_COLOR_LABELS = HAIR_COLOR_LABELS;
}
