import {Component, OnInit, Signal} from '@angular/core';
import {CommonModule} from "@angular/common";
import {IonicModule, ItemReorderEventDetail} from "@ionic/angular";
import {FormsModule} from "@angular/forms";
import {HeaderComponent} from "../header/header.component";
import {WorkerService} from "../../services/worker.service";
import {BehaviorSubject, firstValueFrom, map, Observable} from "rxjs";
import {BODY_TYPE_LABELS} from "../../models/items.model";
import {ActivatedRoute} from "@angular/router";
import {toSignal} from "@angular/core/rxjs-interop";
import {WorkerFullProfile, WorkerPrivateAccount, WorkerProfileUpdate} from "../../models/user.model";
import {WorkerAccountService} from "../../services/worker-account.service";

export interface PhotoItem {
  id: string;
  mainThumbUrl: string;
  previewThumbUrl: string;
  originalUrl: string;
  isMain?: boolean;
}

@Component({
  selector: 'app-profile-management',
  imports: [CommonModule, FormsModule, IonicModule, HeaderComponent],
  templateUrl: './profile-management.component.html',
  styleUrls: ['./profile-management.component.scss'],
  standalone: true
})
export class ProfileManagementComponent implements OnInit {

  activeTab: 'profile' | 'photos' | 'settings' | 'subscription' = 'profile';
  currentUser$ : Observable<WorkerPrivateAccount>;
  currentUser : Signal<WorkerPrivateAccount | undefined>;
  allServices : string[] | undefined;

  // Profile form
  profileForm: WorkerProfileUpdate = {};

  // Photos
  photos: PhotoItem[] = [];
  photosLoading = false;
  uploadingPhoto = false;
  dragOverIndex: number | null = null;
  draggedIndex: number | null = null;

  // Settings
  settingsForm = { username: '', email: '', password: '' };
  settingsPassword2 = '';
  settingsSaved = false;
  settingsError = '';
  savingSettings = false;

  constructor(private accountService: WorkerAccountService, private workerService: WorkerService, private route : ActivatedRoute) {
    this.currentUser$ = this.route.data.pipe(map(data => data['profile']));
    this.currentUser = toSignal(this.currentUser$);
  }

  async ngOnInit(): Promise<void> {
    this.allServices = await firstValueFrom(this.route.data.pipe(map(data => data['services'])));
  }

  setTab(tab: 'profile' | 'photos' | 'settings' | 'subscription') {
    this.activeTab = tab;
    this.settingsSaved = false;
    this.settingsError = '';
  }

  // ── Value modification ─────────────────────────────────────────────────────────

  toggle(param: BehaviorSubject<boolean>) {
    param.next(!param.value);
  }

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
    this.accountService.updateServices(updatedServices).subscribe(() => {
      console.log("updated profile")
    });
  }

  // ── Photos ────────────────────────────────────────────────────────────────

  async onFileSelected(event: any) {
    const file: File = event.target.files[0];
    if (!file) return;

    this.accountService.uploadPhoto(file).subscribe({
      next: (response) => {
        console.log("Upload réussi !", response);
      },
      error: (err) => {
        console.error("Erreur upload", err);
      }
    });
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

  // Drag-and-drop reorder
  onDragStart(index: number) { this.draggedIndex = index; }

  onDragOver(event: DragEvent, index: number) {
    event.preventDefault();
    this.dragOverIndex = index;
  }

  onDrop(targetIndex: number) {
    if (this.draggedIndex === null || this.draggedIndex === targetIndex) return;
    this.draggedIndex  = null;
    this.dragOverIndex = null;
    this.accountService.reorderPhotos(this.photos.map(p => p.id));
  }

  onDragEnd() { this.draggedIndex = null; this.dragOverIndex = null; }

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
}
