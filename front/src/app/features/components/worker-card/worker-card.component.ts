import {ChangeDetectionStrategy, Component, Input, OnDestroy, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {CommonModule} from '@angular/common';
import {IonBadge, IonCard, IonCardContent, IonIcon, IonRippleEffect} from '@ionic/angular/standalone';
import {WorkerService} from '../../services/worker.service';
import {WorkerMinimalProfile} from "../../models/user.model";
import {addIcons} from "ionicons";
import {heart, personCircleOutline, shieldCheckmark} from "ionicons/icons";
import {LanguageManagerService} from "../../services/language-manager.service";
import {AuthService} from "../../services/auth.service";
import {ClientAccountService} from "../../services/client-account.service";
import {Observable, of} from "rxjs";

@Component({
    selector: 'worker-card',
    templateUrl: './worker-card.component.html',
    styleUrls: ['./worker-card.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [CommonModule, IonRippleEffect, IonCard, IonIcon, IonBadge, IonCardContent]
})
export class WorkerCardComponent implements OnInit, OnDestroy {

  @Input() worker!: WorkerMinimalProfile;

  /** Cœur affiché sur la photo si le visiteur (CLIENT) a ce profil en favori. */
  isFavorite$: Observable<boolean> = of(false);

  private hoverDelayId: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private workerService: WorkerService,
    private router: Router,
    protected languageService : LanguageManagerService,
    private authService: AuthService,
    private clientAccountService: ClientAccountService,
  ) {
    addIcons({
      heart, personCircleOutline, shieldCheckmark
    });
  }

  ngOnInit(): void {
    if (this.authService.getUser()?.role === 'CLIENT') {
      this.clientAccountService.ensureLoaded();
      this.isFavorite$ = this.clientAccountService.isFavorite$(this.worker.id);
    }
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
