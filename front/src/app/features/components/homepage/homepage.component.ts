import {Component, OnInit, ViewChild} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {
  IonButton,
  IonCol,
  IonContent, IonFab, IonFabButton,
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
import {closeOutline, locationOutline, optionsOutline, personOutline, arrowUpOutline} from 'ionicons/icons';
import {HeaderComponent} from "../header/header.component";
import {WorkerCardComponent} from "../worker-card/worker-card.component";
import {AgeRangeSelectorComponent} from "../age-range-selector/age-range-selector.component";
import {
  BODY_TYPE_LABELS,
  EYE_COLOR_LABELS,
  HAIR_COLOR_LABELS,
} from "../../models/items.model";
import {WorkerSimpleProfile} from "../../models/user.model";
import {GalleryFilters, GeographicZone} from "../../models/filter.model";
import {WorkerService} from "../../services/worker.service";
import {AuthService} from "../../services/auth.service";
import {debounceTime, distinctUntilChanged, Subject} from "rxjs";
import {ZoneSelectorComponent} from "../zone-selector/zone-selector.component";
import { GalleryStateService } from '../../services/gallery-state.service';
import { sparklesOutline } from 'ionicons/icons';
import {ClientAccountService} from "../../services/client-account.service";
import {KeyValuePipe, NgForOf, NgIf} from "@angular/common";

@Component({
  selector: 'app-homepage',
  templateUrl: './homepage.component.html',
  styleUrls: ['./homepage.component.scss'],
  standalone: true,
  imports: [
    HeaderComponent,
    IonContent,
    IonRefresher,
    IonRefresherContent,
    IonGrid,
    IonRow,
    IonCol,
    ZoneSelectorComponent,
    IonItem,
    IonIcon,
    IonInput,
    IonButton,
    IonSelect,
    IonSelectOption,
    FormsModule,
    WorkerCardComponent,
    IonInfiniteScroll,
    IonInfiniteScrollContent,
    AgeRangeSelectorComponent,
    NgForOf,
    KeyValuePipe,
    NgIf,
    IonFab,
    IonFabButton
  ],
})
export class HomepageComponent implements OnInit {
  @ViewChild(IonContent, { static: false }) content!: IonContent;
  @ViewChild(IonInfiniteScroll) infiniteScroll!: IonInfiniteScroll;

  readonly BODY_TYPE_LABELS = BODY_TYPE_LABELS;
  readonly EYE_COLOR_LABELS = EYE_COLOR_LABELS;
  readonly HAIR_COLOR_LABELS = HAIR_COLOR_LABELS;
  readonly availableLanguages = ['EN', 'FR', 'IT', 'DE', 'ES'];

  showMoreFilters = false;
  isLoading = false;
  noMoreData = false;
  currentPage = 0;
  isLoggedIn = false;
  showScrollTopButton = false;

  childZoneId: number | undefined = undefined;
  parentZoneId: number | undefined = undefined;
  private searchSubject = new Subject<string>();

  allWorkers: WorkerSimpleProfile[] = [];
  allServices: string[] = [];
  parentZones!: GeographicZone[];
  availableChildZones: GeographicZone[] = [];
  filters: GalleryFilters = {};

  constructor(
    private route: ActivatedRoute,
    private workerService: WorkerService,
    private authService: AuthService,
    private stateService: GalleryStateService,
    private clientAccountService: ClientAccountService
  ) {
    addIcons({ optionsOutline, closeOutline, locationOutline, personOutline, sparklesOutline, arrowUpOutline });
  }

  ngOnInit() {
    // Vérification de la session
    this.authService.checkSession().subscribe({
      next: (user) => this.isLoggedIn = user,
      error: () => this.isLoggedIn = false
    });

    // 1. CAS DE RETOUR EN ARRIÈRE (Données déjà présentes dans le State)
    if (this.stateService.allWorkers.length > 0) {
      this.allWorkers = [...this.stateService.allWorkers];
      this.filters = { ...this.stateService.filters };
      this.currentPage = this.stateService.currentPage;
      this.noMoreData = this.stateService.noMoreData;
      this.childZoneId = this.stateService.childZoneId;
      this.parentZoneId = this.stateService.parentZoneId;
      this.showMoreFilters = this.stateService.showMoreFilters;

      // On ne récupère du resolver QUE les données fixes (structures), pas les workers !
      this.route.data.subscribe((data) => {
        this.allServices = data['allServices'] || [];
        this.parentZones = data['locations'] || [];

        setTimeout(async() => {
          if (this.infiniteScroll) {
            this.infiniteScroll.disabled = this.noMoreData;
          }

          if (this.stateService.scrollTop && this.content) {
            await this.content.scrollToPoint(0, this.stateService.scrollTop, 400);
          }
        }, 200);
      });

    } else {
      // 2. PREMIER CHARGEMENT DE LA PAGE
      this.route.data.subscribe((data) => {
        this.allWorkers = data['workers'] || [];
        this.allServices = data['allServices'] || [];
        this.parentZones = data['locations'] || [];

        if (this.allWorkers.length > 0 && this.allWorkers.length < 24) {
          this.noMoreData = true;
          setTimeout(() => { if (this.infiniteScroll) this.infiniteScroll.disabled = true; }, 100);
        } else if (this.allWorkers.length >= 24) {
          this.currentPage = 1; // Prêt pour la page suivante au prochain scroll
        }
        this.saveState();
      });
    }

    // Gestion de la recherche par nom (debounce)
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(searchTerm => {
      this.filters.username = searchTerm;
      this.loadPage(true);
    });
  }

  private saveState(): void {
    this.stateService.allWorkers = this.allWorkers;
    this.stateService.filters = this.filters;
    this.stateService.currentPage = this.currentPage;
    this.stateService.noMoreData = this.noMoreData;
    this.stateService.childZoneId = this.childZoneId;
    this.stateService.parentZoneId = this.parentZoneId;
    this.stateService.showMoreFilters = this.showMoreFilters;
  }

  get activeFilterCount(): number {
    const baseFiltersCount = Object.entries(this.filters).filter(([key, v]) => {
      if (v === undefined || v === null || v === '') return false;
      if (Array.isArray(v)) return v.length > 0;
      return true;
    }).length;

    // Validation stricte des IDs numériques
    const hasParent = this.parentZoneId !== undefined && this.parentZoneId !== null && this.parentZoneId > 0;
    const hasChild = this.childZoneId !== undefined && this.childZoneId !== null && this.childZoneId > 0;

    return baseFiltersCount + (hasParent ? 1 : 0) + (hasChild ? 1 : 0);
  }

  hasActiveFilters(): boolean {
    return this.activeFilterCount > 0;
  }

  async loadUserPreferences(): Promise<void> {
    try {
      const savedFilters = await this.clientAccountService.getSavedPreferences();
      if (savedFilters) {
        this.filters = { ...savedFilters };
        if (savedFilters.zoneId) {
          this.childZoneId = savedFilters.zoneId;
        }
        this.showMoreFilters = true;
        this.loadPage(true);
      }
    } catch (error) {
      console.error("Impossible de charger les préférences :", error);
    }
  }

  clearFilters(): void {
    this.filters = {};
    this.childZoneId = undefined;
    this.parentZoneId = undefined;
    this.availableChildZones = [];
    this.stateService.clear();
    this.loadPage(true);
  }

  loadPage(reset: boolean = false, event?: any): void {
    if (this.isLoading) {
      if (event) event.target.complete();
      return;
    }

    if (reset) {
      this.currentPage = 0;
      this.allWorkers = [];
      this.noMoreData = false;
      if (this.infiniteScroll) this.infiniteScroll.disabled = false;
    }

    this.isLoading = true;
    const requestFilters = { ...this.filters };

    if (this.childZoneId !== undefined && this.childZoneId !== null && String(this.childZoneId) !== '-1') {
      requestFilters.zoneId = this.childZoneId;
    } else if (this.parentZoneId !== undefined && this.parentZoneId !== null && String(this.parentZoneId) !== '-1') {
      requestFilters.zoneId = this.parentZoneId;
    } else {
      delete requestFilters.zoneId;
    }

    // Nettoyage final des filtres
    const cleanFilters = Object.fromEntries(
      Object.entries(requestFilters).filter(([_, v]) => {
        if (v === undefined || v === null || v === '' || v === -1 || String(v) === 'undefined') return false;
        if (Array.isArray(v)) return v.length > 0;
        return true;
      })
    );

    console.log("Filters sent to backend:", cleanFilters);

    this.workerService.getGalleryPage(this.currentPage, cleanFilters).subscribe({
      next: workers => {
        this.allWorkers = reset ? workers : [...this.allWorkers, ...workers];

        // Si le back-end renvoie moins de profils que la taille d'une page (24), le scroll s'arrête
        if (!workers || workers.length < 24) {
          this.noMoreData = true;
          if (this.infiniteScroll) this.infiniteScroll.disabled = true;
        } else {
          this.currentPage++;
        }

        this.saveState();
        if (event) event.target.complete();
        this.isLoading = false;
      },
      error: () => {
        if (event) event.target.complete();
        this.isLoading = false;
      },
    });
  }

  onSearchInput(event: any) {
    const value = event.target.value || '';
    this.searchSubject.next(value.trim());
  }

  async ionViewWillLeave() {
    if (this.content) {
      const scrollElement = await this.content.getScrollElement();
      this.stateService.scrollTop = scrollElement.scrollTop;
    }
  }

  onScroll(event: CustomEvent<any>) {
    const scrollTop = event.detail.scrollTop;

    this.showScrollTopButton = scrollTop > 400;

    // sauvegarde de l'état pour le bouton retour
    if (event.detail && event.detail.scrollTop) {
      this.stateService.scrollTop = event.detail.scrollTop;
    }
  }

  scrollToTop() {
    this.content.scrollToTop(500);
  }

  toggleMoreFilters() { this.showMoreFilters = !this.showMoreFilters; }
  onIonInfinite(event: any): void { this.loadPage(false, event); }
  onRefresh(event: any): void { this.loadPage(true); setTimeout(() => event.target.complete(), 800); }
}
