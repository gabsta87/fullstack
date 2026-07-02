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
  @Input() zones: GeographicZone[] = [];
  @Input() selectedParentId: number | undefined = undefined;
  @Input() selectedChildId: number | undefined = undefined;

  @Input() parentPlaceholder = "Choisir une région...";
  @Input() childPlaceholder = "Choisir une localisation...";

  @Input() allowAllOption = false;
  @Input() allParentsLabel = "Toutes les régions";
  @Input() allChildrenLabel = "Toutes les localisations";

  @Input() showIcons = false;
  @Input() isFilterBanner = false;

  // ── OUTPUTS ───────────────────────────────────────────────────────────────
  @Output() onZoneSelected = new EventEmitter<{ parentZoneId: number | undefined, childZoneId: number | undefined }>();

  // ── VARIABLES INTERNES ────────────────────────────────────────────────────
  parentZoneId: number | undefined = undefined;
  childZoneId: number | undefined = undefined;
  availableChildZones: GeographicZone[] = [];

  constructor() {
    addIcons({ locationOutline, businessOutline });
  }

  ngOnChanges(changes: SimpleChanges): void {
    // Si l'état transmis par le parent change (ex: via le State ou l'initialisation du Resolver)
    if (changes['zones'] || changes['selectedParentId'] || changes['selectedChildId']) {
      this.initializeSelection();
    }
  }

  private initializeSelection() {
    if (!this.zones || this.zones.length === 0) {
      this.resetFields();
      return;
    }

    // Restauration des IDs du parent (Homepage) vers l'état local du select
    this.parentZoneId = this.selectedParentId;
    this.childZoneId = this.selectedChildId;

    // Mise à jour de la liste des enfants disponibles selon le parent sélectionné
    if (this.parentZoneId) {
      const parent = this.zones.find(p => p.id === this.parentZoneId);
      this.availableChildZones = parent?.subZones || [];
    } else {
      this.availableChildZones = [];
    }
  }

  onParentChange() {
    // Si l'utilisateur choisit "Toutes les régions" (valeur nulle ou indéfinie)
    if (!this.parentZoneId) {
      this.resetFields();
    } else {
      // Si l'utilisateur change de parent (ex: Paris -> Lyon), on réinitialise l'enfant
      this.childZoneId = undefined;
      const parent = this.zones.find(p => p.id === this.parentZoneId);
      this.availableChildZones = parent?.subZones || [];
    }
    this.emitSelection();
  }

  onChildChange() {
    if (this.childZoneId === -1) {
      this.childZoneId = undefined;
    }
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
