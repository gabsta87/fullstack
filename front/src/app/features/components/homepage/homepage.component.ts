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

  readonly bodyTypesList = BODY_TYPES_LIST;
  readonly BODY_TYPE_LABELS = BODY_TYPE_LABELS;
  readonly EYE_COLOR_LABELS = EYE_COLOR_LABELS;
  readonly HAIR_COLOR_LABELS = HAIR_COLOR_LABELS;

  showMoreFilters = false;
  isLoading = false;
  noMoreData = false;
  currentPage = 0; // Commencer à 0 pour correspondre à Spring Boot (Page 0)
  childZoneId : number = -1;
  parentZoneId : number = -1;

  allWorkers: WorkerSimpleProfile[] = [];
  allServices: string[] = [];
  parentZones!: GeographicZone[];
  availableChildZones: GeographicZone[] = [];

  // Objet unique et propre aligné avec le HTML et le Back-end
  filters: GalleryFilters = {};

  constructor(
    private route: ActivatedRoute,
    private workerService: WorkerService,
    public authService: AuthService,
  ) {
    addIcons({ optionsOutline, closeOutline, locationOutline });
  }

  ngOnInit() {
    this.authService.checkSession().subscribe();

    this.route.data.subscribe((data) => {
      this.allWorkers = data['workers'] || [];
      this.allServices = data['allServices'] || [];
      this.parentZones = data['locations'] || [];
    });

  }

  toggleMoreFilters() {
    this.showMoreFilters = !this.showMoreFilters;
  }

  get activeFilterCount(): number {
    return Object.values(this.filters).filter(v => v !== undefined && v !== null && v !== '').length;
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

  private loadPage(reset: boolean = false): void {
    if (this.isLoading) return;

    if (reset) {
      this.currentPage = 0;
      this.allWorkers = [];
      this.noMoreData = false;
      if (this.infiniteScroll) this.infiniteScroll.disabled = false;
    }

    this.isLoading = true;

    const requestFilters = { ...this.filters };

    if (this.childZoneId > -1) {
      requestFilters.zoneId = this.childZoneId;
    } else if (this.parentZoneId > -1) {
      requestFilters.zoneId = this.parentZoneId;
    }

    // Nettoyage strict
    const cleanFilters = Object.fromEntries(
      Object.entries(requestFilters).filter(([_, v]) => v !== undefined && v !== null && v !== '' && v !== -1)
    );


    console.log("fitlers : ",cleanFilters)

    this.workerService.getGalleryPage(this.currentPage, cleanFilters).subscribe({
      next: workers => {
        this.allWorkers = reset ? workers : [...this.allWorkers, ...workers];
        if (!workers || !workers.length) {
          this.noMoreData = true;
        } else {
          this.currentPage++;
        }
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; },
    });
  }

  onParentZoneChange() {
    // 1. Reset de la sous-localisation précédemment sélectionnée
    this.childZoneId = -1;

    // 2. Trouver l'objet parent sélectionné pour extraire ses sous-zones rattachées
    const selectedParent = this.parentZones.find(z => z.id === this.parentZoneId);
    this.availableChildZones = selectedParent && selectedParent.subZones ? selectedParent.subZones : [];

    // 3. Lancer la recherche automatique
    this.applyFilters();
  }

  onIonInfinite(event: any): void {
    this.loadPage(false);
    setTimeout(() => event.target.complete(), 500);
  }

  onRefresh(event: any): void {
    this.loadPage(true);
    setTimeout(() => event.target.complete(), 800);
  }
}
