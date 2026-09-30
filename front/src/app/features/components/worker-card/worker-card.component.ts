import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy} from '@angular/core';
import {Router} from '@angular/router';
import {CommonModule} from '@angular/common';
import {
  IonBadge,
  IonCard,
  IonCardHeader,
  IonCardSubtitle,
  IonCardTitle,
  IonIcon,
  IonRippleEffect
} from '@ionic/angular/standalone';
import {WorkerService} from '../../services/worker.service';
import {WorkerSimpleProfile} from "../../models/user.model";
import {addIcons} from "ionicons";
import {personCircleOutline} from "ionicons/icons";

@Component({
  selector: 'worker-card',
  templateUrl: './worker-card.component.html',
  styleUrls: ['./worker-card.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, IonRippleEffect, IonCard, IonIcon, IonBadge, IonCardHeader, IonCardTitle, IonCardSubtitle],
})
export class WorkerCardComponent implements OnDestroy {

  @Input() worker!: WorkerSimpleProfile;

  private hoverDelayId: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private workerService: WorkerService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {
    addIcons({
      personCircleOutline
    });
  }

  // ── Hover ────────────────────────────────________________──────────────────

  onMouseEnter(): void {
    // On garde le prefetch intelligent en arrière-plan si l'utilisateur survole la carte
    this.hoverDelayId = setTimeout(() => {
      this.workerService.prefetchProfile(this.worker.id);
    }, 300);
  }

  onMouseLeave(): void {
    if (this.hoverDelayId) clearTimeout(this.hoverDelayId);
  }

  // ── Navigation ─────────────────────────────────────────────────────────────

  navigateToProfile(): void {
    this.router.navigate(['/profile'], {
      queryParams: { id: this.worker.id },
      state: { from: this.router.url }
    });
  }

  // ── Display URL ────────────────────────────────____________________________

  get displayUrl(): string | null {
    return this.worker.mainThumbUrl || null;
  }

  ngOnDestroy(): void {
    if (this.hoverDelayId) clearTimeout(this.hoverDelayId);
  }
}
