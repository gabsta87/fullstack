import { Component, OnInit } from '@angular/core';
import { AccountService, AccountMe, WorkerProfileUpdate } from '../../services/account.service';
import {CommonModule} from "@angular/common";
import {IonicModule} from "@ionic/angular";
import {FormsModule} from "@angular/forms";
import {HeaderComponent} from "../header/header.component";
import {WorkerService} from "../../services/worker.service";
import {Observable} from "rxjs";
import {BodyType} from "../../models/worker.model";

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

  me: AccountMe | null = null;
  activeTab: 'profile' | 'photos' | 'settings' | 'subscription' = 'profile';

  // Profile form
  profileForm: WorkerProfileUpdate = {};
  selectedServices: string[] = [];
  allServices : Observable<string[]>;
  profileSaved = false;
  profileError = '';
  savingProfile = false;

  // Availability
  togglingAvailability = false;

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

  loading = true;
  public bodyTypeOptions: BodyType[] = ['SLIM', 'ATHLETIC', 'AVERAGE', 'CURVY', 'ROUND'];

  constructor(private accountService: AccountService, private workerService: WorkerService) {
    this.allServices = this.workerService.getWorkersServices();
  }

  ngOnInit(): void {
    this.accountService.getMe().subscribe({
      next: me => {
        this.me = me;
        this.profileForm = {
          description: me.description ?? '',
          location:    me.location    ?? '',
          region:      me.region      ?? '',
          height:      me.height      ?? 0,
          weight:      me.weight      ?? 0,
          bodyType:    me.bodyType    ?? '',
        };
        this.selectedServices = me.services ? [...me.services] : [];
        this.settingsForm.username = me.username;
        this.settingsForm.email    = me.email;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  setTab(tab: 'profile' | 'photos' | 'settings' | 'subscription') {
    this.activeTab = tab;
    this.profileSaved = false;
    this.profileError = '';
    this.settingsSaved = false;
    this.settingsError = '';
    if (tab === 'photos' && this.photos.length === 0) this.loadPhotos();
  }

  // ── Availability ─────────────────────────────────────────────────────────

  toggleAvailability() {
    if (!this.me || this.togglingAvailability) return;
    this.togglingAvailability = true;
    const next = !this.me.available;
    this.accountService.setAvailability(next).subscribe({
      next: res => {
        if (this.me) this.me.available = res.available;
        this.togglingAvailability = false;
      },
      error: () => { this.togglingAvailability = false; }
    });
  }

  // ── Profile form ─────────────────────────────────────────────────────────

  toggleService(svc: string) {
    const idx = this.selectedServices.indexOf(svc);
    if (idx >= 0) this.selectedServices.splice(idx, 1);
    else          this.selectedServices.push(svc);
  }

  isServiceSelected(svc: string): boolean {
    return this.selectedServices.includes(svc);
  }

  saveProfile() {
    this.profileError  = '';
    this.profileSaved  = false;
    this.savingProfile = true;

    const payload: WorkerProfileUpdate = {
      ...this.profileForm,
      services: this.selectedServices
    };

    this.accountService.updateProfile(payload).subscribe({
      next: () => {
        this.profileSaved  = true;
        this.savingProfile = false;
      },
      error: () => {
        this.profileError  = 'Erreur lors de la sauvegarde.';
        this.savingProfile = false;
      }
    });
  }

  // ── Photos ────────────────────────────────────────────────────────────────

  loadPhotos() {
    // Re-use /account/me which returns mainThumbUrl.
    // For the full photo list we call the worker public endpoint.
    // Since the worker is logged in, we use their own id.
    if (!this.me) return;
    this.photosLoading = true;
    // GET /workers/{id} returns WorkerProfileDTO with photos[]
    // We'll just re-use the public endpoint here.
    // A dedicated /account/worker/photos endpoint can be added later.
    // For now, use HttpClient directly — inject it via AccountService or here.
    this.photosLoading = false;
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    this.uploadPhoto(file);
    input.value = ''; // reset so same file can be re-selected
  }

  uploadPhoto(file: File) {
    this.uploadingPhoto = true;
    this.accountService.uploadPhoto(file).subscribe({
      next: photo => {
        this.photos.unshift({ ...photo, isMain: this.photos.length === 0 });
        this.uploadingPhoto = false;
        if (this.me && !this.me.mainThumbUrl) this.me.mainThumbUrl = photo.mainThumbUrl;
      },
      error: () => { this.uploadingPhoto = false; }
    });
  }

  setMain(photo: PhotoItem) {
    this.accountService.setMainPhoto(photo.id).subscribe(() => {
      this.photos.forEach(p => p.isMain = p.id === photo.id);
      if (this.me) this.me.mainThumbUrl = photo.mainThumbUrl;
    });
  }

  deletePhoto(photo: PhotoItem) {
    this.accountService.deletePhoto(photo.id).subscribe(() => {
      const idx = this.photos.indexOf(photo);
      this.photos.splice(idx, 1);
      if (photo.isMain && this.photos.length > 0) {
        this.photos[0].isMain = true;
        if (this.me) this.me.mainThumbUrl = this.photos[0].mainThumbUrl;
      }
    });
  }

  // Drag-and-drop reorder
  onDragStart(index: number) { this.draggedIndex = index; }

  onDragOver(event: DragEvent, index: number) {
    event.preventDefault();
    this.dragOverIndex = index;
  }

  onDrop(targetIndex: number) {
    if (this.draggedIndex === null || this.draggedIndex === targetIndex) return;
    const moved = this.photos.splice(this.draggedIndex, 1)[0];
    this.photos.splice(targetIndex, 0, moved);
    this.draggedIndex  = null;
    this.dragOverIndex = null;
    this.accountService.reorderPhotos(this.photos.map(p => p.id)).subscribe();
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
    this.accountService.updateSettings(payload).subscribe({
      next: () => {
        this.settingsSaved = true;
        this.savingSettings = false;
        this.settingsForm.password = '';
        this.settingsPassword2 = '';
        if (this.me) {
          this.me.username = payload.username ?? this.me.username;
          this.me.email    = payload.email    ?? this.me.email;
        }
      },
      error: () => {
        this.settingsError = 'Erreur lors de la sauvegarde.';
        this.savingSettings = false;
      }
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  get subscriptionDaysLeft(): number {
    return this.me?.subscriptionDaysLeft ?? 0;
  }

  get subscriptionColor(): string {
    if (this.subscriptionDaysLeft <= 0)  return 'danger';
    if (this.subscriptionDaysLeft <= 7)  return 'warning';
    return 'success';
  }

  get subscriptionLabel(): string {
    if (this.subscriptionDaysLeft <= 0) return 'Abonnement expiré';
    return `${this.subscriptionDaysLeft} jour${this.subscriptionDaysLeft > 1 ? 's' : ''} restant${this.subscriptionDaysLeft > 1 ? 's' : ''}`;
  }

  protected readonly Math = Math;
}
