import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { locationOutline, businessOutline } from 'ionicons/icons';

export interface GeographicZone {
  id: number;
  name: string;
  subZones?: GeographicZone[];
}

@Component({
  selector: 'app-zone-selector',
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule],
  templateUrl: './zone-selector.component.html',
  styleUrls: ['./zone-selector.component.scss']
})
export class ZoneSelectorComponent implements OnChanges {
  // ── INPUTS CONFIGURATION ──────────────────────────────────────────────────
  @Input() zones: GeographicZone[] = [];           // La liste complète issue du Resolver
  @Input() selectedChildId?: number;                // L'ID de la ville actuellement sauvegardé

  @Input() parentPlaceholder = "Choisir une région...";
  @Input() childPlaceholder = "Choisir une localisation...";

  @Input() allowAllOption = false;                  // True pour la Homepage (filtres globaux)
  @Input() allParentsLabel = "Toutes les régions";
  @Input() allChildrenLabel = "Toutes les localisations";

  @Input() showIcons = false;                       // True pour le look "Banner" de la Homepage
  @Input() isFilterBanner = false;                  // Gère la disposition CSS (Grille de filtres vs Formulaire)

  // ── OUTPUTS ───────────────────────────────────────────────────────────────
  @Output() onZoneSelected = new EventEmitter<{ parentZoneId: number | undefined, childZoneId: number | undefined }>();

  // ── VARIABLES INTERNES ────────────────────────────────────────────────────
  parentZoneId: number | undefined;
  childZoneId: number | undefined;
  availableChildZones: GeographicZone[] = [];

  constructor() {
    addIcons({ locationOutline, businessOutline });
  }

  ngOnChanges(changes: SimpleChanges): void {
    // Dès que les zones ou l'ID sélectionné changent (chargement asynchrone), on initialise l'arborescence
    if (changes['zones'] || changes['selectedChildId']) {
      this.initializeSelection();
    }
  }

  private initializeSelection() {
    if (!this.zones || this.zones.length === 0) return;

    // Si on a un ID enfant (ex: 42), on cherche la région qui possède cet enfant dans ses subZones
    if (this.selectedChildId && this.selectedChildId !== -1) {
      const parent = this.zones.find(p =>
        p.subZones?.some(c => c.id === this.selectedChildId)
      );

      if (parent) {
        this.parentZoneId = parent.id;
        this.availableChildZones = parent.subZones || [];
        this.childZoneId = this.selectedChildId;
        return;
      }
    }

    // Si aucun ID enfant n'est trouvé ou fourni, on reset le composant à l'état initial
    this.resetFields();
  }

  onParentChange() {
    this.childZoneId = undefined; // Reset de la ville enfant en cas de changement de région

    if (this.parentZoneId) {
      const parent = this.zones.find(p => p.id === this.parentZoneId);
      this.availableChildZones = parent?.subZones || [];
    } else {
      this.availableChildZones = [];
    }

    this.emitSelection();
  }

  onChildChange() {
    this.emitSelection();
  }

  private emitSelection() {
    this.onZoneSelected.emit({
      parentZoneId: this.parentZoneId,
      childZoneId: this.childZoneId
    });
  }

  private resetFields() {
    this.parentZoneId = undefined;
    this.childZoneId = undefined;
    this.availableChildZones = [];
  }
}
