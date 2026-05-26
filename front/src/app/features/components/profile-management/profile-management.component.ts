import {Component, OnInit, Signal} from '@angular/core';
import { AccountService, WorkerProfileUpdate } from '../../services/account.service';
import {CommonModule} from "@angular/common";
import {IonicModule} from "@ionic/angular";
import {FormsModule} from "@angular/forms";
import {HeaderComponent} from "../header/header.component";
import {WorkerService} from "../../services/worker.service";
import {BehaviorSubject, firstValueFrom, map, Observable} from "rxjs";
import {BODY_TYPE_LABELS, WorkerProfile} from "../../models/worker.model";
import {ActivatedRoute} from "@angular/router";
import {toSignal} from "@angular/core/rxjs-interop";

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
  currentUser$ : Observable<WorkerProfile>;
  currentUser : Signal<WorkerProfile | undefined>;
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

  constructor(private accountService: AccountService, private workerService: WorkerService, private route : ActivatedRoute) {
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

  toggleService(profile: WorkerProfile, service: string) {
    // Copie ou récupération de la liste actuelle
    let currentServices = [...(profile.services || [])];

    if (currentServices.includes(service)) {
      // Si le service y est, on le retire
      currentServices = currentServices.filter(s => s !== service);
    } else {
      // Sinon, on l'ajoute
      currentServices.push(service);
    }

    // Ici, tu appelles ton service pour mettre à jour directement la DB.
    // Exemple si tu as une méthode de mise à jour partielle :
    this.accountService.updateProfile({ services: currentServices }).subscribe({
      next: () => {
        // Optionnel : Si ton currentUser$ ne se recharge pas automatiquement
        // depuis le serveur après le changement, tu peux mettre à jour localement l'objet
        profile.services = currentServices;
      },
      error: (err) => {
        console.error('Erreur lors de la mise à jour du service', err);
      }
    });
  }

  // ── Photos ────────────────────────────────────────────────────────────────

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    this.uploadPhoto(file);
    input.value = ''; // reset so same file can be re-selected
  }

  uploadPhoto(file: File) {
    this.uploadingPhoto = true;
    this.accountService.uploadPhoto(file);
  }

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
    this.accountService.updateSettings(payload);
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
