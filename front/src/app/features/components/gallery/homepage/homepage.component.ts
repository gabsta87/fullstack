// src/app/features/gallery/homepage/homepage.component.ts

import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  IonContent, IonInfiniteScroll, IonInfiniteScrollContent,
  IonRefresher, IonRefresherContent, IonHeader, IonToolbar,
  IonTitle, IonButtons, IonButton, IonIcon, IonChip, IonLabel,
  IonSelect, IonSelectOption, IonSearchbar,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { optionsOutline, closeOutline, locationOutline } from 'ionicons/icons';

import { WorkerGalleryDTO, GalleryFilters } from '../../../models/worker.model';
import { WorkerService } from '../../../services/worker.service';
import { WorkerCardComponent } from '../worker-card/worker-card.component';

@Component({
  selector: 'app-homepage',
  templateUrl: './homepage.component.html',
  styleUrls: ['./homepage.component.scss'],
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    IonContent, IonInfiniteScroll, IonInfiniteScrollContent,
    IonRefresher, IonRefresherContent, IonHeader, IonToolbar,
    IonTitle, IonButtons, IonButton, IonIcon, IonChip, IonLabel,
    IonSelect, IonSelectOption, IonSearchbar,
    WorkerCardComponent,
  ],
})
export class HomepageComponent implements OnInit {

  @ViewChild(IonInfiniteScroll) infiniteScroll!: IonInfiniteScroll;
  @ViewChild(IonContent)        ionContent!: IonContent;

  // ── State ──────────────────────────────────────────────────────────────────
  availableWorkers:   WorkerGalleryDTO[] = [];
  unavailableWorkers: WorkerGalleryDTO[] = [];

  currentPage  = 0;
  isLoading    = false;
  noMoreData   = false;

  filtersOpen  = false;

  filters: GalleryFilters = {};

  // ── Filter options ─────────────────────────────────────────────────────────
  readonly regions    = ['Paris','Lyon','Marseille','Bordeaux','Toulouse','Nice','Nantes','Strasbourg'];
  readonly bodyTypes  = ['Mince','Athlétique','Normale','Pulpeuse','Ronde'];
  readonly services   = ['Standard','Premium']; // TODO: fill from backend
  readonly eyeColors  = [
    { label: 'Marron',   hex: '#6b3d2e' },
    { label: 'Noisette', hex: '#9b6a3a' },
    { label: 'Vert',     hex: '#4a7c59' },
    { label: 'Bleu',     hex: '#4a7aaf' },
    { label: 'Gris',     hex: '#8a9aaa' },
  ];
  readonly hairColors = [
    { label: 'Noir',    hex: '#1a1410' },
    { label: 'Brun',    hex: '#5c3d2e' },
    { label: 'Châtain', hex: '#8b5e3c' },
    { label: 'Blond',   hex: '#d4a847' },
    { label: 'Roux',    hex: '#c04a1a' },
  ];

  // Temp filter state (applied only on "Apply" / immediate for region)
  tempFilters: {
    bodyType:  Record<string, boolean>;
    services:  Record<string, boolean>;
    eyeColor:  string;
    hairColor: string;
    heightMin: number | null;
    heightMax: number | null;
    weightMin: number | null;
    weightMax: number | null;
  } = {
    bodyType:  {},
    services:  {},
    eyeColor:  '',
    hairColor: '',
    heightMin: null,
    heightMax: null,
    weightMin: null,
    weightMax: null,
  };

  constructor(private workerService: WorkerService) {
    addIcons({ optionsOutline, closeOutline, locationOutline });
    this.bodyTypes.forEach(b => this.tempFilters.bodyType[b] = false);
    this.services .forEach(s => this.tempFilters.services[s] = false);
  }

  ngOnInit(): void {
    this.loadPage(true);
  }

  // ── Data loading ───────────────────────────────────────────────────────────

  loadPage(reset: boolean): void {
    if (this.isLoading) return;
    if (reset) {
      this.currentPage        = 0;
      this.availableWorkers   = [];
      this.unavailableWorkers = [];
      this.noMoreData         = false;
    }

    this.isLoading = true;

    this.workerService.getGalleryPage(this.currentPage, this.filters).subscribe({
      next: (workers) => {
        if (!workers.length) {
          this.noMoreData = true;
          if (this.infiniteScroll) this.infiniteScroll.disabled = true;
        } else {
          this.availableWorkers   = [...this.availableWorkers,
            ...workers.filter(w =>  w.available)];
          this.unavailableWorkers = [...this.unavailableWorkers,
            ...workers.filter(w => !w.available)];
          this.currentPage++;
        }
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; },
    });
  }

  onIonInfinite(event: any): void {
    this.loadPage(false);
    setTimeout(() => event.target.complete(), 500);
  }

  onRefresh(event: any): void {
    if (this.infiniteScroll) this.infiniteScroll.disabled = false;
    this.loadPage(true);
    setTimeout(() => event.target.complete(), 800);
  }

  // ── Filters ────────────────────────────────────────────────────────────────

  onRegionChange(): void {
    this.loadPage(true);
  }

  toggleFilters(): void {
    this.filtersOpen = !this.filtersOpen;
  }

  applyFilters(): void {
    this.filters = {
      ...this.filters,
      bodyType:  Object.keys(this.tempFilters.bodyType) .filter(k => this.tempFilters.bodyType[k]),
      services:  Object.keys(this.tempFilters.services) .filter(k => this.tempFilters.services[k]),
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
    this.tempFilters.heightMin = null;
    this.tempFilters.heightMax = null;
    this.tempFilters.weightMin = null;
    this.tempFilters.weightMax = null;
    this.loadPage(true);
  }

  toggleSwatch(field: 'eyeColor' | 'hairColor', label: string): void {
    this.tempFilters[field] = this.tempFilters[field] === label ? '' : label;
  }

  get activeFilterCount(): number {
    let n = 0;
    if (this.filters.region)                    n++;
    if (this.filters.eyeColor)                  n++;
    if (this.filters.hairColor)                 n++;
    if (this.filters.heightMin != null || this.filters.heightMax != null) n++;
    if (this.filters.weightMin != null || this.filters.weightMax != null) n++;
    n += (this.filters.bodyType  ?? []).length;
    n += (this.filters.services  ?? []).length;
    return n;
  }
}
