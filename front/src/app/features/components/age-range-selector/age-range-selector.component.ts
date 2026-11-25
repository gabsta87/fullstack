import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges, ChangeDetectionStrategy } from '@angular/core';
import { IonItem, IonRange } from '@ionic/angular/standalone';
import {LanguageManagerService} from "../../services/language-manager.service";
import { AsyncPipe } from "@angular/common";

@Component({
    selector: 'age-range-selector',
    templateUrl: './age-range-selector.component.html',
    styleUrls: ['./age-range-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [IonItem, IonRange, AsyncPipe]
})
export class AgeRangeSelectorComponent implements OnInit, OnChanges {
  @Input() minAge: string | undefined = undefined;
  @Input() maxAge: string | undefined = undefined;

  @Output() minAgeChange = new EventEmitter<string | undefined>();
  @Output() maxAgeChange = new EventEmitter<string | undefined>();
  @Output() ageRangeChange = new EventEmitter<{ minAge: string | undefined; maxAge: string | undefined }>();

  // Single local source of truth for the label and the slider
  currentLower: number = 18;
  currentUpper: number = 60;

  constructor(protected languageService:LanguageManagerService) {}

  ngOnInit() {
    this.syncInputsToLocal();
  }

  // When the parent changes the filters (e.g. reset button)
  ngOnChanges(changes: SimpleChanges) {
    if (changes['minAge'] || changes['maxAge']) {
      this.syncInputsToLocal();
    }
  }

  private syncInputsToLocal() {
    this.currentLower = this.minAge ? parseInt(this.minAge, 10) : 18;
    this.currentUpper = this.maxAge ? parseInt(this.maxAge, 10) : 60;
  }

  // 1. Called CONTINUOUSLY (smooth label and bubble movement)
  onRangeInput(event: any) {
    const value = event.detail.value;
    if (!value) return;

    this.currentLower = value.lower;
    this.currentUpper = value.upper;
  }

// 2. Called ONLY on release (sends the value to the parent)
  onRangeChangeEnd(event: any) {
    const value = event.detail.value;
    if (!value) return;

    this.currentLower = value.lower;
    this.currentUpper = value.upper;

    const lowerStr = this.currentLower.toString();
    const upperStr = this.currentUpper === 60 ? undefined : this.currentUpper.toString();

    this.minAgeChange.emit(lowerStr);
    this.maxAgeChange.emit(upperStr);

    // Emit through the single emitter
    this.ageRangeChange.emit({ minAge: lowerStr, maxAge: upperStr });
  }
}
