import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {
  IonContent,
  IonInfiniteScroll,
  IonInfiniteScrollContent,
  IonRefresher,
  IonRefresherContent
} from '@ionic/angular/standalone';
import {addIcons} from 'ionicons';
import {closeOutline, locationOutline, optionsOutline} from 'ionicons/icons';

import {WorkerService} from '../../services/worker.service';
import {WorkerCardComponent} from '../worker-card/worker-card.component';
import {AuthService} from "../../services/auth.service";
import {HeaderComponent} from '../header/header.component';
import {
  BODY_TYPES_LIST, BodyType,
  EYE_COLORS,
  GalleryFilters,
  HAIR_COLORS,
  REGIONS,
  WorkerGalleryDTO
} from '../../models/worker.model';
import {map} from "rxjs";

@Component({
  selector: 'app-homepage',
  templateUrl: './homepage.component.html',
  styleUrls: ['./homepage.component.scss'],
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    IonContent, IonInfiniteScroll, IonInfiniteScrollContent,
    IonRefresher, IonRefresherContent,HeaderComponent,
    WorkerCardComponent
  ],
})
export class HomepageComponent implements OnInit {

  @ViewChild(IonInfiniteScroll) infiniteScroll!: IonInfiniteScroll;

  readonly bodyTypesList = BODY_TYPES_LIST;
  readonly regions    = REGIONS;
  readonly eyeColors  = EYE_COLORS;
  readonly hairColors = HAIR_COLORS;

  selectionStates: {
    bodyType: Record<string, boolean>;
    services: Record<string, boolean>;
  } = { bodyType: {}, services: {} };

  allWorkers :        WorkerGalleryDTO[] = [];
  availableWorkers:   WorkerGalleryDTO[] = [];
  unavailableWorkers: WorkerGalleryDTO[] = [];
  allServices: string[] = [];

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
  ) {
    addIcons({ optionsOutline, closeOutline, locationOutline });
  }

  ngOnInit(): void {
    this.authService.checkSession().subscribe();
    this.route.data.subscribe((data) => {
      this.allWorkers = data['workers'] || [];
      this.allServices = data['allServices'] || [];

      // 2. Initialisation dynamique des états des filtres
      this.initFilterStates();
    });
    this.splitAndAppend(this.allWorkers);
  }

  // ── Data loading ───────────────────────────────────────────────────────────

  private initFilterStates() {
    // Initialisation des BodyTypes (toujours présents via ton modèle)
    this.bodyTypesList.forEach(b => {
      if (this.selectionStates.bodyType[b] === undefined) {
        this.selectionStates.bodyType[b] = false;
      }
    });

    // Initialisation des Services chargés dynamiquement
    this.allServices.forEach(s => {
      if (this.selectionStates.services[s] === undefined) {
        this.selectionStates.services[s] = false;
      }
    });
  }

  /** Called by infinite scroll and pull-to-refresh for pages > 0 */
  private loadPage(reset: boolean = false): void {
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
    this.bodyTypesList.forEach(b => this.selectionStates.bodyType[b] = false);
    this.allServices.forEach(s => this.selectionStates.services[s] = false);
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

}
