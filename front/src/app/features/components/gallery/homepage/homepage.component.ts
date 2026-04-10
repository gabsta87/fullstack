import { Component, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  IonContent, IonInfiniteScroll, IonInfiniteScrollContent,
  IonRefresher, IonRefresherContent, IonHeader, IonToolbar,
  IonTitle, IonButtons, IonButton, IonIcon, IonSpinner,
  IonModal
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { optionsOutline, closeOutline, locationOutline } from 'ionicons/icons';

import { WorkerGalleryDTO, GalleryFilters } from '../../../models/worker.model';
import { WorkerService } from '../../../services/worker.service';
import { WorkerCardComponent } from '../worker-card/worker-card.component';
import {AuthService} from "../../../services/auth.service";
import {HttpClient} from "@angular/common/http";

@Component({
  selector: 'app-homepage',
  templateUrl: './homepage.component.html',
  styleUrls: ['./homepage.component.scss'],
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    IonContent, IonInfiniteScroll, IonInfiniteScrollContent,
    IonRefresher, IonRefresherContent, IonHeader, IonToolbar,
    IonTitle, IonButtons, IonButton, IonIcon, IonSpinner,
    IonModal,
    WorkerCardComponent,
  ],
})
export class HomepageComponent implements OnInit {

  @ViewChild(IonInfiniteScroll) infiniteScroll!: IonInfiniteScroll;

  availableWorkers:   WorkerGalleryDTO[] = [];
  unavailableWorkers: WorkerGalleryDTO[] = [];

  currentPage = 1; // page 0 already loaded by resolver
  isLoading   = false;
  noMoreData  = false;
  filtersOpen = false;
  filters: GalleryFilters = {};

  readonly regions   = ['Paris','Lyon','Marseille','Bordeaux','Toulouse','Nice','Nantes','Strasbourg'];
  readonly bodyTypes = ['Mince','Athlétique','Normale','Pulpeuse','Ronde'];
  readonly services  = ['Standard','Premium'];
  readonly eyeColors = [
    { label: 'Marron',   hex: '#6b3d2e' }, { label: 'Noisette', hex: '#9b6a3a' },
    { label: 'Vert',     hex: '#4a7c59' }, { label: 'Bleu',     hex: '#4a7aaf' },
    { label: 'Gris',     hex: '#8a9aaa' },
  ];
  readonly hairColors = [
    { label: 'Noir',    hex: '#1a1410' }, { label: 'Brun',    hex: '#5c3d2e' },
    { label: 'Châtain', hex: '#8b5e3c' }, { label: 'Blond',   hex: '#d4a847' },
    { label: 'Roux',    hex: '#c04a1a' },
  ];

  tempFilters: {
    bodyType: Record<string, boolean>;
    services: Record<string, boolean>;
    eyeColor: string; hairColor: string;
    heightMin: number | null; heightMax: number | null;
    weightMin: number | null; weightMax: number | null;
  } = {
    bodyType: {}, services: {},
    eyeColor: '', hairColor: '',
    heightMin: null, heightMax: null,
    weightMin: null, weightMax: null,
  };

  constructor(
    private route: ActivatedRoute,
    private workerService: WorkerService,
    private authService: AuthService,
    private http: HttpClient
  ) {
    addIcons({ optionsOutline, closeOutline, locationOutline });
    this.bodyTypes.forEach(b => this.tempFilters.bodyType[b] = false);
    this.services .forEach(s => this.tempFilters.services[s] = false);
  }

  ngOnInit(): void {
    // Page 0 was already fetched by galleryResolver — use it directly,
    // no extra HTTP call needed on first render.
    const resolved: WorkerGalleryDTO[] = this.route.snapshot.data['workers'] ?? [];
    this.splitAndAppend(resolved);
  }

  // ── Data loading ───────────────────────────────────────────────────────────

  /** Called by infinite scroll and pull-to-refresh for pages > 0 */
  private loadPage(reset: boolean): void {
    if (this.isLoading) return;
    if (reset) {
      this.currentPage        = 0;
      this.availableWorkers   = [];
      this.unavailableWorkers = [];
      this.noMoreData         = false;
      if (this.infiniteScroll) this.infiniteScroll.disabled = false;
    }

    this.isLoading = true;
    this.workerService.getGalleryPage(this.currentPage, this.filters).subscribe({
      next: workers => {
        if (!workers.length) {
          this.noMoreData = true;
          if (this.infiniteScroll) this.infiniteScroll.disabled = true;
        } else {
          this.splitAndAppend(workers);
          this.currentPage++;
        }
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; },
    });
  }

  private splitAndAppend(workers: WorkerGalleryDTO[]): void {
    this.availableWorkers   = [...this.availableWorkers,   ...workers.filter(w =>  w.available)];
    this.unavailableWorkers = [...this.unavailableWorkers, ...workers.filter(w => !w.available)];
  }

  onIonInfinite(event: any): void {
    this.loadPage(false);
    setTimeout(() => event.target.complete(), 500);
  }

  onRefresh(event: any): void {
    this.loadPage(true);
    setTimeout(() => event.target.complete(), 800);
  }

  // ── Filters ────────────────────────────────────────────────────────────────

  onRegionChange(): void { this.loadPage(true); }
  toggleFilters():  void { this.filtersOpen = !this.filtersOpen; }

  applyFilters(): void {
    this.filters = {
      ...this.filters,
      bodyType:  Object.keys(this.tempFilters.bodyType).filter(k => this.tempFilters.bodyType[k]),
      services:  Object.keys(this.tempFilters.services).filter(k => this.tempFilters.services[k]),
      eyeColor:  this.tempFilters.eyeColor  || undefined,
      hairColor: this.tempFilters.hairColor || undefined,
      heightMin: this.tempFilters.heightMin ?? undefined,
      heightMax: this.tempFilters.heightMax ?? undefined,
      weightMin: this.tempFilters.weightMin ?? undefined,
      weightMax: this.tempFilters.weightMax ?? undefined,
    };
    this.filtersOpen = false;
    this.loadPage(true);
  }

  clearFilters(): void {
    this.filters = {};
    this.bodyTypes.forEach(b => this.tempFilters.bodyType[b] = false);
    this.services .forEach(s => this.tempFilters.services[s] = false);
    this.tempFilters.eyeColor  = '';
    this.tempFilters.hairColor = '';
    this.tempFilters.heightMin = this.tempFilters.heightMax = null;
    this.tempFilters.weightMin = this.tempFilters.weightMax = null;
    this.loadPage(true);
  }

  toggleSwatch(field: 'eyeColor' | 'hairColor', label: string): void {
    this.tempFilters[field] = this.tempFilters[field] === label ? '' : label;
  }

  get activeFilterCount(): number {
    let n = 0;
    if (this.filters.region)    n++;
    if (this.filters.eyeColor)  n++;
    if (this.filters.hairColor) n++;
    if (this.filters.heightMin != null || this.filters.heightMax != null) n++;
    if (this.filters.weightMin != null || this.filters.weightMax != null) n++;
    n += (this.filters.bodyType ?? []).length;
    n += (this.filters.services ?? []).length;
    return n;
  }

  // Modal state
  loginOpen    = false;
  registerOpen = false;

// Login form
  loginPseudo   = '';
  loginPassword = '';
  loginError    = '';
  loginLoading  = false;

// Register form
  registerRole    : 'client' | 'worker' = 'client';
  registerTouched = false;
  registerLoading = false;
  registerError   = '';
  registerSuccess = '';
  registerForm = { username: '', email: '', password: '', confirmPassword: '' };

  openLogin()    { this.loginOpen    = true; }
  openRegister() { this.registerOpen = true; }

  switchToRegister() { this.loginOpen = false;    this.registerOpen = true; }
  switchToLogin()    { this.registerOpen = false; this.loginOpen    = true; }

  openResetPassword() {
    this.loginOpen = false;
    // TODO: open reset modal or navigate
  }

  submitLogin() {
    this.loginError  = '';
    this.loginLoading = true;
    this.authService.login(this.loginPseudo, this.loginPassword, '').subscribe({
      next: () => {
        this.loginLoading = false;
        this.loginOpen    = false;
        // optionally reload workers or update nav
      },
      error: (err) => {
        this.loginLoading = false;
        this.loginError   = err.status === 401 ? 'Identifiants incorrects.' : 'Erreur serveur.';
      }
    });
  }

  submitRegister() {
    this.registerTouched = true;
    this.registerError   = '';
    this.registerSuccess = '';

    const f = this.registerForm;
    if (!f.username || f.username.length < 3) return;
    if (!f.email)                             return;
    if (!f.password || f.password.length < 6) return;
    if (f.password !== f.confirmPassword)     return;

    this.registerLoading = true;
    const endpoint = this.registerRole === 'worker'
      ? '/auth/register/worker'
      : '/auth/register/client';

    this.http.post(endpoint, f, { withCredentials: true, responseType: 'text' }).subscribe({
      next: () => {
        this.registerLoading = false;
        this.registerSuccess = 'Compte créé ! Vous pouvez maintenant vous connecter.';
        setTimeout(() => this.switchToLogin(), 1500);
      },
      error: (err) => {
        this.registerLoading = false;
        this.registerError = err.error ?? 'Erreur lors de l\'inscription.';
      }
    });
  }
}
