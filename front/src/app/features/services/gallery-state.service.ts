import { Injectable } from '@angular/core';
import {WorkerSimpleProfile} from "../models/user.model";
import {GalleryFilters} from "../models/filter.model";

@Injectable({
  providedIn: 'root'
})
export class GalleryStateService {
  // On y stocke exactement les variables de ta Homepage
  allWorkers: WorkerSimpleProfile[] = [];
  filters: GalleryFilters = {};
  currentPage = 0;
  noMoreData = false;
  childZoneId: number | undefined = undefined;
  parentZoneId: number | undefined = undefined;
  showMoreFilters = false;

  // Permet de vider le cache proprement (ex: clic sur "Effacer")
  clear(): void {
    this.allWorkers = [];
    this.filters = {};
    this.currentPage = 0;
    this.noMoreData = false;
    this.childZoneId = undefined;
    this.parentZoneId = undefined;
    this.showMoreFilters = false;
  }
}
