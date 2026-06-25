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
} from "../../models/items.model";
import {WorkerSimpleProfile} from "../../models/user.model";
import {GalleryFilters, GeographicZone} from "../../models/filter.model";
import {WorkerService} from "../../services/worker.service";
import {AuthService} from "../../services/auth.service";
import {debounceTime, distinctUntilChanged, Subject} from "rxjs";
import {ZoneSelectorComponent} from "../zone-selector/zone-selector.component";

@Component({
  selector: 'app-homepage',
  templateUrl: './homepage.component.html',
  styleUrls: ['./homepage.component.scss'],
  standalone: true,
  imports: [
    CommonModule, FormsModule, IonContent, IonInfiniteScroll, IonInfiniteScrollContent,
    IonRefresher, IonRefresherContent, HeaderComponent, WorkerCardComponent,
    IonGrid, IonRow, IonCol, IonItem, IonSelect, IonSelectOption, IonIcon, IonInput, IonButton,
    ZoneSelectorComponent
  ],
})
export class HomepageComponent implements OnInit {

  @ViewChild(IonInfiniteScroll) infiniteScroll!: IonInfiniteScroll;

  readonly BODY_TYPE_LABELS = BODY_TYPE_LABELS;
  readonly EYE_COLOR_LABELS = EYE_COLOR_LABELS;
  readonly HAIR_COLOR_LABELS = HAIR_COLOR_LABELS;

  showMoreFilters = false;
  isLoading = false;
  noMoreData = false;
  currentPage = 0;
  childZoneId : number | undefined = undefined;
  parentZoneId : number | undefined = undefined;
  private searchSubject = new Subject<string>();

  allWorkers: WorkerSimpleProfile[] = [];
  allServices: string[] = [];
  parentZones!: GeographicZone[];
  availableChildZones: GeographicZone[] = [];

  // Objet unique et propre aligné avec le HTML et le Back-end
  filters: GalleryFilters = {};

  constructor(
    private route: ActivatedRoute,
    private workerService: WorkerService,
    private authService: AuthService,
  ) {
    addIcons({ optionsOutline, closeOutline, locationOutline });
  }

  ngOnInit() {
    this.authService.checkSession().subscribe();

    this.route.data.subscribe((data) => {
      this.allWorkers = data['workers'] || [];
      this.allServices = data['allServices'] || [];
      this.parentZones = data['locations'] || [];

      if (this.allWorkers.length > 0) {
        if (this.allWorkers.length < 24) {
          this.noMoreData = true;
          setTimeout(() => {
            if (this.infiniteScroll) this.infiniteScroll.disabled = true;
          }, 100);
        } else {
          this.currentPage = 1;
        }
      }
    });

    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(searchTerm => {
      this.filters.username = searchTerm;
      this.applyFilters();
    });
  }

  onSearchInput(event: any) {
    const value = event.target.value || '';
    this.searchSubject.next(value.trim());
  }

  toggleMoreFilters() {
    this.showMoreFilters = !this.showMoreFilters;
  }

  get activeFilterCount(): number {
    const baseFiltersCount = Object.values(this.filters).filter(
      v => v !== undefined && v !== null && v !== ''
    ).length;

    const hasParent = typeof this.parentZoneId === 'number' && this.parentZoneId > -1;
    const hasChild = typeof this.childZoneId === 'number' && this.childZoneId > -1;

    return baseFiltersCount + (hasParent ? 1 : 0) + (hasChild ? 1 : 0);
  }

  hasActiveFilters(): boolean {
    return this.activeFilterCount > 0;
  }
  applyFilters(): void {
    this.loadPage(true);
  }

  clearFilters(): void {
    this.filters = {};
    this.childZoneId = -1;
    this.parentZoneId = -1;
    this.availableChildZones = [];
    this.applyFilters();
  }

  private loadPage(reset: boolean = false, event?: any): void {
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

    if (this.childZoneId != undefined) {
      requestFilters.zoneId = this.childZoneId;
    } else if (this.parentZoneId != undefined) {
      requestFilters.zoneId = this.parentZoneId;
    }

    const cleanFilters = Object.fromEntries(
      Object.entries(requestFilters).filter(([_, v]) => v !== undefined && v !== null && v !== '' && v !== -1)
    );

    this.workerService.getGalleryPage(this.currentPage, cleanFilters).subscribe({
      next: workers => {
        this.allWorkers = reset ? workers : [...this.allWorkers, ...workers];

        if (!workers || workers.length < 24) {
          this.noMoreData = true;
          if (this.infiniteScroll) this.infiniteScroll.disabled = true;
        } else {
          this.currentPage++;
        }

        if (event) event.target.complete();
        this.isLoading = false;
      },
      error: () => {
        if (event) event.target.complete();
        this.isLoading = false;
      },
    });
  }

  onIonInfinite(event: any): void {
    // 🎯 On délègue la responsabilité du "complete" à la méthode qui gère le réseau
    this.loadPage(false, event);
  }

  onRefresh(event: any): void {
    this.loadPage(true);
    setTimeout(() => event.target.complete(), 800);
  }
}
