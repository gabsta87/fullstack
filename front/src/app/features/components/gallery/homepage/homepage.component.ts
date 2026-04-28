import {ChangeDetectorRef, Component, OnInit, ViewChild} from '@angular/core';
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

import { WorkerService } from '../../../services/worker.service';
import { WorkerCardComponent } from '../worker-card/worker-card.component';
import { AuthService } from "../../../services/auth.service";
import { RegisterService } from "../../../services/register.service";
import { HeaderComponent } from '../../header/header.component';
import { REGIONS, BODY_TYPES, SERVICES, EYE_COLORS, HAIR_COLORS, WorkerGalleryDTO, GalleryFilters } from '../../../models/worker.model';

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
    IonModal,HeaderComponent,
    WorkerCardComponent
  ],
})
export class HomepageComponent implements OnInit {

  @ViewChild(IonInfiniteScroll) infiniteScroll!: IonInfiniteScroll;

  readonly regions    = REGIONS;
  readonly bodyTypes  = BODY_TYPES;
  readonly workersProvidedServices   = SERVICES;
  readonly eyeColors  = EYE_COLORS;
  readonly hairColors = HAIR_COLORS;

  selectionStates = {
    bodyType: {} as Record<string, boolean>,
    services: {} as Record<string, boolean>
  };

  availableWorkers:   WorkerGalleryDTO[] = [];
  unavailableWorkers: WorkerGalleryDTO[] = [];

  currentPage = 1; // page 0 already loaded by resolver
  isLoading   = false;
  noMoreData  = false;
  filtersOpen = false;
  filters: GalleryFilters = {};

  tempFilters: GalleryFilters = {
    region: undefined,
    eyeColor: undefined,
    hairColor: undefined,
    heightMin: null,
    heightMax: null,
    weightMin: null,
    weightMax: null,
  };

  constructor(
    private route: ActivatedRoute,
    private workerService: WorkerService,
    public authService: AuthService,
    private registerService: RegisterService,
    private cdr: ChangeDetectorRef
  ) {
    addIcons({ optionsOutline, closeOutline, locationOutline });

    // Initialisation propre des états de cases à cocher
    this.bodyTypes.forEach(b => this.selectionStates.bodyType[b] = false);
    this.workersProvidedServices.forEach(s => this.selectionStates.services[s] = false);
  }

  ngOnInit(): void {
    this.authService.checkSession().subscribe();
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

  onLogout() {
    this.authService.logout().subscribe({
      next: () => {
        this.cdr.detectChanges();
        // TODO : redirect to gallery
        console.log("TODO : Redirect to gallery")
      },
      error: () => {
        this.cdr.detectChanges();
      },
    });
  }

  openAccount() {
    // TODO : redirect to account
    console.log("TODO : Redirect to account")
  }

  // ── Filters ────────────────────────────────────────────────────────────────

  onRegionChange(): void { this.loadPage(true); }

  toggleFilter(list: any[] | undefined, value: string) {
    if (!list) return;
    const index = list.indexOf(value);
    if (index > -1) {
      list.splice(index, 1);
    } else {
      list.push(value);
    }
  }

  applyFilters(): void {
    const selectedBody = Object.keys(this.selectionStates.bodyType).filter(k => this.selectionStates.bodyType[k]);
    const selectedServ = Object.keys(this.selectionStates.services).filter(k => this.selectionStates.services[k]);

    this.filters = {
      region: this.tempFilters.region || undefined,
      bodyType: selectedBody.length > 0 ? selectedBody : undefined,
      services: selectedServ.length > 0 ? selectedServ : undefined,
      eyeColor: this.tempFilters.eyeColor || undefined,
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
    // Reset selection UI
    this.bodyTypes.forEach(b => this.selectionStates.bodyType[b] = false);
    this.workersProvidedServices.forEach(s => this.selectionStates.services[s] = false);
    // Reset temp fields
    this.tempFilters = { region: undefined, eyeColor: undefined, hairColor: undefined };
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

    const call = this.registerRole === 'worker'
      ? this.registerService.registerWorker(f.username, f.email, f.password)
      : this.registerService.registerClient(f.username, f.email, f.password);

    call.subscribe({
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
