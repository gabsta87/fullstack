import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface Worker {
  id: number;
  name: string;
  age: number;
  location: string;
  region: string;
  bodyType: string;
  eyeColor: string;
  hairColor: string;
  height: number;
  weight: number;
  services: string[];
  available: boolean;
  lastRefreshed: Date; // order: most recent first
  photoUrl?: string;
}

@Component({
  selector: 'app-homepage',
  standalone: true,
  imports: [RouterLink, CommonModule, FormsModule],
  templateUrl: './homepage.component.html',
  styleUrl: './homepage.component.scss'
})

export class HomepageComponent implements OnInit{
  // ── Filter state ────────────────────────────────────────────────────────────
  filtersOpen = false;

  filters: {
    region: string;
    eyeColor: string;
    hairColor: string;
    bodyType: Record<string, boolean>;
    services: Record<string, boolean>;
    heightMin: number | null;
    heightMax: number | null;
    weightMin: number | null;
    weightMax: number | null;
  } = {
    region: '',
    eyeColor: '',
    hairColor: '',
    bodyType: {},
    services: {},
    heightMin: null,
    heightMax: null,
    weightMin: null,
    weightMax: null,
  };

  // ── Options ─────────────────────────────────────────────────────────────────
  regions = ['Paris', 'Lyon', 'Marseille', 'Bordeaux', 'Toulouse', 'Nice', 'Nantes', 'Strasbourg'];

  options = {
    bodyType: ['Mince', 'Athlétique', 'Normale', 'Pulpeuse', 'Ronde'],
    services: ['Standard', 'Premium'],  // TODO: fill this list
    eyeColor: [
      { label: 'Marron',  hex: '#6b3d2e' },
      { label: 'Noisette',hex: '#9b6a3a' },
      { label: 'Vert',    hex: '#4a7c59' },
      { label: 'Bleu',    hex: '#4a7aaf' },
      { label: 'Gris',    hex: '#8a9aaa' },
    ],
    hairColor: [
      { label: 'Noir',    hex: '#1a1410' },
      { label: 'Brun',    hex: '#5c3d2e' },
      { label: 'Châtain', hex: '#8b5e3c' },
      { label: 'Blond',   hex: '#d4a847' },
      { label: 'Roux',    hex: '#c04a1a' },
      { label: 'Coloré',  hex: 'linear-gradient(135deg,#f093fb,#4facfe)' },
    ],
  };

  // ── Placeholder data (replace with API call) ────────────────────────────────
  private allWorkers: Worker[] = [
    { id:1,  name:'Amélie',  age:26, location:'Paris 8e',  region:'Paris',     bodyType:'Athlétique', eyeColor:'Vert',    hairColor:'Brun',   height:168, weight:56, services:['Premium'],           available:true,  lastRefreshed: new Date('2024-01-10T14:30:00') },
    { id:2,  name:'Sofia',   age:23, location:'Lyon 2e',   region:'Lyon',      bodyType:'Mince',      eyeColor:'Bleu',    hairColor:'Blond',  height:172, weight:53, services:['Standard','Premium'], available:true,  lastRefreshed: new Date('2024-01-10T14:00:00') },
    { id:3,  name:'Léa',     age:29, location:'Paris 11e', region:'Paris',     bodyType:'Normale',    eyeColor:'Marron',  hairColor:'Châtain',height:165, weight:60, services:['Standard'],           available:true,  lastRefreshed: new Date('2024-01-10T13:45:00') },
    { id:4,  name:'Camille', age:25, location:'Bordeaux',  region:'Bordeaux',  bodyType:'Pulpeuse',   eyeColor:'Noisette',hairColor:'Noir',   height:162, weight:65, services:['Premium'],           available:true,  lastRefreshed: new Date('2024-01-10T13:00:00') },
    { id:5,  name:'Inès',    age:31, location:'Marseille', region:'Marseille', bodyType:'Mince',      eyeColor:'Marron',  hairColor:'Noir',   height:170, weight:54, services:['Standard','Premium'], available:true,  lastRefreshed: new Date('2024-01-10T12:30:00') },
    { id:6,  name:'Zoé',     age:24, location:'Nice',      region:'Nice',      bodyType:'Athlétique', eyeColor:'Vert',    hairColor:'Blond',  height:169, weight:57, services:['Standard'],           available:false, lastRefreshed: new Date('2024-01-10T11:00:00') },
    { id:7,  name:'Manon',   age:27, location:'Toulouse',  region:'Toulouse',  bodyType:'Normale',    eyeColor:'Bleu',    hairColor:'Châtain',height:164, weight:59, services:['Premium'],           available:false, lastRefreshed: new Date('2024-01-10T10:30:00') },
    { id:8,  name:'Clara',   age:22, location:'Nantes',    region:'Nantes',    bodyType:'Ronde',      eyeColor:'Gris',    hairColor:'Roux',   height:160, weight:68, services:['Standard'],           available:false, lastRefreshed: new Date('2024-01-10T09:00:00') },
  ];

  availableWorkers:   Worker[] = [];
  unavailableWorkers: Worker[] = [];

  // ── Lifecycle ────────────────────────────────────────────────────────────────
  ngOnInit(): void {
    // Initialize checkbox maps
    this.options.bodyType.forEach(o => this.filters.bodyType[o] = false);
    this.options.services.forEach(s => this.filters.services[s] = false);
    this.applyFilters();
  }

  // ── Filter logic ─────────────────────────────────────────────────────────────
  applyFilters(): void {
    const activeBodyTypes = Object.keys(this.filters.bodyType).filter(k => this.filters.bodyType[k]);
    const activeServices  = Object.keys(this.filters.services).filter(k => this.filters.services[k]);

    const filtered = this.allWorkers.filter(w => {
      if (this.filters.region    && w.region    !== this.filters.region)    return false;
      if (this.filters.eyeColor  && w.eyeColor  !== this.filters.eyeColor)  return false;
      if (this.filters.hairColor && w.hairColor !== this.filters.hairColor) return false;
      if (activeBodyTypes.length && !activeBodyTypes.includes(w.bodyType))  return false;
      if (activeServices.length  && !activeServices.some(s => w.services.includes(s))) return false;
      if (this.filters.heightMin != null && w.height < this.filters.heightMin) return false;
      if (this.filters.heightMax != null && w.height > this.filters.heightMax) return false;
      if (this.filters.weightMin != null && w.weight < this.filters.weightMin) return false;
      if (this.filters.weightMax != null && w.weight > this.filters.weightMax) return false;
      return true;
    });

    // Sort by lastRefreshed descending within each availability group
    const byDate = (a: Worker, b: Worker) =>
      b.lastRefreshed.getTime() - a.lastRefreshed.getTime();

    this.availableWorkers   = filtered.filter(w =>  w.available).sort(byDate);
    this.unavailableWorkers = filtered.filter(w => !w.available).sort(byDate);
  }

  get activeFilterCount(): number {
    let count = 0;
    if (this.filters.region)    count++;
    if (this.filters.eyeColor)  count++;
    if (this.filters.hairColor) count++;
    if (this.filters.heightMin != null || this.filters.heightMax != null) count++;
    if (this.filters.weightMin != null || this.filters.weightMax != null) count++;
    count += Object.values(this.filters.bodyType).filter(Boolean).length;
    count += Object.values(this.filters.services).filter(Boolean).length;
    return count;
  }

  toggleFilters(): void { this.filtersOpen = !this.filtersOpen; }

  toggle(field: 'eyeColor' | 'hairColor', value: string): void {
    (this.filters[field] as string) = this.filters[field] === value ? '' : value;
    this.applyFilters();
  }

  clearFilters(): void {
    this.filters.region    = '';
    this.filters.eyeColor  = '';
    this.filters.hairColor = '';
    this.filters.heightMin = null;
    this.filters.heightMax = null;
    this.filters.weightMin = null;
    this.filters.weightMax = null;
    this.options.bodyType.forEach(o => this.filters.bodyType[o] = false);
    this.options.services.forEach(s => this.filters.services[s] = false);
    this.applyFilters();
  }

}
