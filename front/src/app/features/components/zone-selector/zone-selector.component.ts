import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { locationOutline, businessOutline } from 'ionicons/icons';
import { GeographicZone } from "../../models/filter.model";
import {LanguageManagerService} from "../../services/language-manager.service";

@Component({
    selector: 'zone-selector',
    imports: [CommonModule, FormsModule, IonicModule],
    templateUrl: './zone-selector.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrls: ['./zone-selector.component.scss']
})
export class ZoneSelectorComponent implements OnChanges {
  // ── INPUTS CONFIGURATION ──────────────────────────────────────────────────
  @Input() zones: GeographicZone[] = [];
  @Input() selectedParentId: number | undefined = undefined;
  @Input() selectedChildId: number | undefined = undefined;

  @Input() parentPlaceholder = "";
  @Input() childPlaceholder = "";

  @Input() allowAllOption = false;
  @Input() allParentsLabel = "";
  @Input() allChildrenLabel = "";

  @Input() showIcons = false;
  @Input() isFilterBanner = false;

  // ── OUTPUTS ───────────────────────────────────────────────────────────────
  @Output() onZoneSelected = new EventEmitter<{ parentZoneId: number | undefined, childZoneId: number | undefined }>();

  // ── VARIABLES INTERNES ────────────────────────────────────────────────────
  parentZoneId: number | undefined = undefined;
  childZoneId: number | undefined = undefined;
  availableChildZones: GeographicZone[] = [];

  constructor(protected langService: LanguageManagerService) {
    addIcons({ locationOutline, businessOutline });
  }

  ngOnChanges(changes: SimpleChanges): void {
    // When the state passed by the parent changes (e.g. restored state or resolver init)
    if (changes['zones'] || changes['selectedParentId'] || changes['selectedChildId']) {
      this.initializeSelection();
    }
  }

  private initializeSelection() {
    if (!this.zones || this.zones.length === 0) {
      this.resetFields();
      return;
    }

    // Restore the parent's (homepage) ids into the select's local state
    this.parentZoneId = this.selectedParentId;
    this.childZoneId = this.selectedChildId;

    // Update the available child zones for the selected parent
    if (this.parentZoneId) {
      const parent = this.zones.find(p => p.id === this.parentZoneId);
      this.availableChildZones = parent?.subZones || [];
    } else {
      this.availableChildZones = [];
    }
  }

  onParentChange() {
    // "All regions" selected (null or undefined value)
    if (!this.parentZoneId) {
      this.resetFields();
    } else {
      // Another parent selected (e.g. Paris -> Lyon): reset the child
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
