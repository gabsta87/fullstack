import {Component, OnInit, ViewChild} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {
  IonButton,
  IonCol,
  IonContent,
  IonGrid,
  IonIcon,
  IonInfiniteScroll,
  IonInfiniteScrollContent,
  IonInput,
  IonItem,
  IonRefresher,
  IonRefresherContent,
  IonRow,
  IonSelect,
  IonSelectOption
} from '@ionic/angular/standalone';
import {addIcons} from 'ionicons';
import {closeOutline, locationOutline, optionsOutline} from 'ionicons/icons';
import {HeaderComponent} from "../header/header.component";
import {WorkerCardComponent} from "../worker-card/worker-card.component";
import {
  BODY_TYPE_LABELS,
  BODY_TYPES_LIST,
  EYE_COLOR_LABELS,
  HAIR_COLOR_LABELS,
  REGIONS
} from "../../models/items.model";
import {WorkerSimpleProfile} from "../../models/user.model";
import {GalleryFilters} from "../../models/filter.model";
import {WorkerService} from "../../services/worker.service";
import {AuthService} from "../../services/auth.service";


@Component({
  selector: 'app-homepage',
  templateUrl: './homepage.component.html',
  styleUrls: ['./homepage.component.scss'],
  standalone: true,
  imports: [
    CommonModule, FormsModule, IonContent, IonInfiniteScroll, IonInfiniteScrollContent,
    IonRefresher, IonRefresherContent, HeaderComponent, WorkerCardComponent,
    IonGrid, IonRow, IonCol, IonItem, IonSelect, IonSelectOption, IonIcon, IonInput, IonButton
  ],
})
export class HomepageComponent implements OnInit {

  @ViewChild(IonInfiniteScroll) infiniteScroll!: IonInfiniteScroll;

  // Injection des constantes pour le Template HTML
  readonly regions = REGIONS;
  readonly bodyTypesList = BODY_TYPES_LIST;
  readonly BODY_TYPE_LABELS = BODY_TYPE_LABELS;
  readonly EYE_COLOR_LABELS = EYE_COLOR_LABELS;
  readonly HAIR_COLOR_LABELS = HAIR_COLOR_LABELS;

  // États de l'IHM
  showMoreFilters = false;
  filtersOpen = false;
  isLoading = false;
  noMoreData = false;
  currentPage = 1;

  // Données
  allWorkers: WorkerSimpleProfile[] = [];
  availableWorkers: WorkerSimpleProfile[] = [];
  unavailableWorkers: WorkerSimpleProfile[] = [];
  allServices: string[] = [];

  // 1. Filtres "Simples" temporaires (Saisies textuelles ou Select uniques)
  tempFilters: {
    region?: string;
    location?: string;
    eyeColor?: string;
    hairColor?: string;
    name?: string;
  } = {};

  // 2. Filtres "Multiples" (Checkbox / Swatches cochés à true/false)
  selectionStates: {
    bodyType: Record<string, boolean>;
    services: Record<string, boolean>;
  } = { bodyType: {}, services: {} };

  // 3. Objet final envoyé au serveur suite à l'application
  filters: GalleryFilters = {};

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

      // Initialisation dynamique des structures d'états
      this.initFilterStates();
      this.splitAndAppend(this.allWorkers);
    });
  }

  /**
   * Initialisation automatique des structures de choix multiples
   */
  private initFilterStates(): void {
    this.bodyTypesList.forEach(b => {
      if (this.selectionStates.bodyType[b] === undefined) {
        this.selectionStates.bodyType[b] = false;
      }
    });

    this.allServices.forEach(s => {
      if (this.selectionStates.services[s] === undefined) {
        this.selectionStates.services[s] = false;
      }
    });
  }

  // ── Gestion Universelle des Filtres (Itérations Dynamiques) ────────────────

  toggleMoreFilters() {
    this.showMoreFilters = !this.showMoreFilters;
  }

  /**
   * Compte le nombre total de filtres actifs de manière 100% générique.
   * Scanne les chaînes de caractères remplies et compte le nombre d'éléments dans les tableaux.
   */
  get activeFilterCount(): number {
    let count = 0;

    Object.values(this.filters).forEach(value => {
      if (Array.isArray(value)) {
        count += value.length; // Nombre d'éléments cochés dans les listes multiples
      } else if (value !== undefined && value !== null && value !== '') {
        count++; // Champ texte ou select simple configuré
      }
    });

    return count;
  }

  /**
   * Indique si au moins un filtre est appliqué
   */
  hasActiveFilters(): boolean {
    return this.activeFilterCount > 0;
  }

  /**
   * Applique les filtres en transformant les formulaires d'IHM en DTO pour le serveur
   */
  applyFilters(): void {
    // Extraction automatique des clés cochées à "true"
    const selectedBody = Object.keys(this.selectionStates.bodyType).filter(k => this.selectionStates.bodyType[k]);
    const selectedServ = Object.keys(this.selectionStates.services).filter(k => this.selectionStates.services[k]);

    // On bâtit l'objet final dynamiquement
    this.filters = {
      region: this.tempFilters.region || undefined,
      location: this.tempFilters.location || undefined,
      eyeColor: this.tempFilters.eyeColor || undefined,
      hairColor: this.tempFilters.hairColor || undefined,
      bodyType: selectedBody.length > 0 ? selectedBody : undefined,
      services: selectedServ.length > 0 ? selectedServ : undefined,
    };

    this.filtersOpen = false;
    this.loadPage(true); // Recharge la galerie depuis la page 0
  }

  /**
   * Reset Universel : Parcourt toutes les structures de données par boucle
   * pour tout remettre à zéro sans écrire les propriétés une par une.
   */
  clearFilters(): void {
    // 1. Remise à blanc de tous les champs simples (tempFilters)
    Object.keys(this.tempFilters).forEach(key => {
      this.tempFilters[key as keyof typeof this.tempFilters] = undefined;
    });

    // 2. Remise à "false" de toutes les cases cochées de manière générique
    Object.keys(this.selectionStates).forEach(category => {
      const record = this.selectionStates[category as keyof typeof this.selectionStates];
      Object.keys(record).forEach(key => record[key] = false);
    });

    // 3. Application du reset global et rechargement
    this.applyFilters();
  }

  /**
   * Sélection rapide type Bouton/Tag (ex: Couleur yeux/cheveux)
   */
  toggleSwatch(field: keyof typeof this.tempFilters, label: string): void {
    this.tempFilters[field] = this.tempFilters[field] === label ? undefined : label;
  }

  onRegionChange(): void {
    this.applyFilters();
  }

  // ── Chargement des données et Pagination ───────────────────────────────────

  private loadPage(reset: boolean = false): void {
    if (this.isLoading) return;

    if (reset) {
      this.currentPage = 0;
      this.availableWorkers = [];
      this.unavailableWorkers = [];
      this.noMoreData = false;
      if (this.infiniteScroll) this.infiniteScroll.disabled = false;
    }

    this.isLoading = true;
    this.workerService.getGalleryPage(this.currentPage, this.filters).subscribe({
      next: workers => {
        if (!workers || !workers.length) {
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

  private splitAndAppend(workers: WorkerSimpleProfile[]): void {
    this.availableWorkers = [...this.availableWorkers, ...workers.filter(w => w.available)];
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

  protected readonly REGIONS = REGIONS;
}
