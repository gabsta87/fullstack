import { Component, EventEmitter, Input, Output } from '@angular/core';
import {IonItem, IonLabel, IonRange} from "@ionic/angular/standalone";

@Component({
  selector: 'age-range-selector',
  templateUrl: './age-range-selector.component.html',
  styleUrls: ['./age-range-selector.component.scss'],
  imports: [
    IonItem,
    IonLabel,
    IonRange
  ],
  standalone: true
})
export class AgeRangeSelectorComponent {
  @Input() minAge: string | undefined = undefined;
  @Input() maxAge: string | undefined = undefined;

  @Output() minAgeChange = new EventEmitter<string | undefined>();
  @Output() maxAgeChange = new EventEmitter<string | undefined>();
  @Output() ionChange = new EventEmitter<void>();

  // Getters pour alimenter l'affichage visuel et le range
  get currentLower(): number {
    return this.minAge ? parseInt(this.minAge, 10) : 18;
  }

  get currentUpper(): number {
    return this.maxAge ? parseInt(this.maxAge, 10) : 60;
  }

  get rangeValue() {
    return { lower: this.currentLower, upper: this.currentUpper };
  }

  onRangeChange(event: any) {
    const value = event.detail.value;
    if (!value) return;

    const lowerStr = value.lower.toString();
    // 🎯 RÈGLE : Si l'upper atteint 60, on transmet undefined (qui sera ignoré/nullifié par le serveur)
    const upperStr = value.upper === 60 ? undefined : value.upper.toString();

    this.minAgeChange.emit(lowerStr);
    this.maxAgeChange.emit(upperStr);

    // On notifie le composant parent qu'un changement a eu lieu
    this.ionChange.emit();
  }
}
