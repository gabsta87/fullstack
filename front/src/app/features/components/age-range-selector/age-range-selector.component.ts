import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { IonItem, IonLabel, IonRange } from '@ionic/angular/standalone';

@Component({
  selector: 'age-range-selector',
  templateUrl: './age-range-selector.component.html',
  styleUrls: ['./age-range-selector.component.scss'],
  imports: [IonItem, IonLabel, IonRange],
  standalone: true
})
export class AgeRangeSelectorComponent implements OnInit, OnChanges {
  @Input() minAge: string | undefined = undefined;
  @Input() maxAge: string | undefined = undefined;

  @Output() minAgeChange = new EventEmitter<string | undefined>();
  @Output() maxAgeChange = new EventEmitter<string | undefined>();
  @Output() ageRangeChange = new EventEmitter<{ minAge: string | undefined; maxAge: string | undefined }>();

  // 🎯 L'unique source de vérité locale pour l'affichage et le curseur
  currentLower: number = 18;
  currentUpper: number = 60;

  ngOnInit() {
    this.syncInputsToLocal();
  }

  // Si le parent modifie les filtres extérieurement (ex: bouton reset)
  ngOnChanges(changes: SimpleChanges) {
    if (changes['minAge'] || changes['maxAge']) {
      this.syncInputsToLocal();
    }
  }

  private syncInputsToLocal() {
    this.currentLower = this.minAge ? parseInt(this.minAge, 10) : 18;
    this.currentUpper = this.maxAge ? parseInt(this.maxAge, 10) : 60;
  }

  // 1. Appelé EN CONTINU (Mouvement fluide du texte et de la bulle)
  onRangeInput(event: any) {
    const value = event.detail.value;
    if (!value) return;

    this.currentLower = value.lower;
    this.currentUpper = value.upper;
  }

// 2. Appelé UNIQUEMENT au relâchement (Envoi officiel au parent)
  onRangeChangeEnd(event: any) {
    const value = event.detail.value;
    if (!value) return;

    this.currentLower = value.lower;
    this.currentUpper = value.upper;

    const lowerStr = this.currentLower.toString();
    const upperStr = this.currentUpper === 60 ? undefined : this.currentUpper.toString();

    this.minAgeChange.emit(lowerStr);
    this.maxAgeChange.emit(upperStr);

    // 🎯 On émet via notre nouvel émetteur unique
    this.ageRangeChange.emit({ minAge: lowerStr, maxAge: upperStr });
  }
}
