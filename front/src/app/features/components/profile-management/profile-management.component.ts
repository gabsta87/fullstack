import {Component, OnInit} from '@angular/core';
import {CommonModule} from "@angular/common";
import {IonicModule, ItemReorderEventDetail} from "@ionic/angular";
import {FormsModule} from "@angular/forms";
import {HeaderComponent} from "../header/header.component";
import {firstValueFrom, map, Observable} from "rxjs";
import {BODY_TYPE_LABELS, PhotoItem, EYE_COLOR_LABELS, HAIR_COLOR_LABELS, REGIONS} from "../../models/items.model";
import {ActivatedRoute} from "@angular/router";
import {WorkerFullProfile, WorkerPrivateAccount, WorkerProfileUpdate} from "../../models/user.model";
import {WorkerAccountService} from "../../services/worker-account.service";
import {tap} from "rxjs/operators";
import {addIcons} from "ionicons";
import {addCircleOutline, camera, move, trashOutline} from "ionicons/icons";

@Component({
  selector: 'app-profile-management',
  imports: [CommonModule, FormsModule, IonicModule, HeaderComponent],
  templateUrl: './profile-management.component.html',
  styleUrls: ['./profile-management.component.scss'],
  standalone: true
})
export class ProfileManagementComponent implements OnInit {

  activeTab     : 'profile' | 'photos' | 'settings' | 'subscription' = 'profile';

  currentUser$! : Observable<WorkerPrivateAccount>;
  allServices!  : string[];
  photos!       : PhotoItem[];

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
      addCircleOutline,trashOutline,move, camera
    });
  }

  async ngOnInit(): Promise<void> {
    this.allServices = await firstValueFrom(this.route.data.pipe(map(data => data['services'])));

    // 1. Récupérer les données initiales du compte fournies par le Resolver
    const initialProfile = await firstValueFrom(this.route.data.pipe(map(data => data['profile'])));
    if (initialProfile) {
      this.profileForm = {
        bodyType: initialProfile.bodyType,
        location: initialProfile.location,
        description: initialProfile.description
      };
    }

    // 2. Écouter le flux SSE temps réel pour les futures mises à jour (ex: photos)
    this.currentUser$ = this.accountService.listenToMyAccount().pipe(
      tap(user => {
        if (user && user.photos) {
          this.photos = user.photos;
        }
      })
    );
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
    console.log(`Mise à jour du champ [${field}] avec la valeur :`, value);
    try {
      // On utilise la syntaxe JavaScript de propriété calculée en envoyant
      // uniquement l'objet partiel : { bodyType: '...' } ou { location: '...' }
      await this.accountService.updateProfile({ [field]: value });
    } catch (error) {
      console.error(`Erreur lors de la mise à jour du champ ${field}`, error);
    }
  }

  // ── Photos ────────────────────────────────────────────────────────────────

  async onFileSelected(event: any) {
    const file: File = event.target.files[0];
    if (!file) return;

    this.accountService.uploadPhoto(file);
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
  protected readonly EYE_COLOR_LABELS = EYE_COLOR_LABELS;
  protected readonly HAIR_COLOR_LABELS = HAIR_COLOR_LABELS;
  protected readonly REGIONS = REGIONS;
}
