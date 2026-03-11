
import {
  Component, Input, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef
} from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { IonCard, IonRippleEffect } from '@ionic/angular/standalone';
import { WorkerGalleryDTO } from '../../../models/worker.model';
import { WorkerService } from '../../../services/worker.service';

@Component({
  selector: 'app-worker-card',
  templateUrl: './worker-card.component.html',
  styleUrls: ['./worker-card.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, IonCard, IonRippleEffect],
})
export class WorkerCardComponent implements OnDestroy {

  @Input() worker!: WorkerGalleryDTO;

  // Hover carousel state
  previewUrls: string[]  = [];
  currentPreviewIdx      = 0;
  isHovering             = false;
  previewLoaded          = false;

  private intervalId: ReturnType<typeof setInterval> | null = null;
  private hoverDelayId: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private workerService: WorkerService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  // ── Hover handlers ─────────────────────────────────────────────────────────

  onMouseEnter(): void {
    this.isHovering = true;

    // Small delay so quick mouse-overs don't trigger fetches
    this.hoverDelayId = setTimeout(() => {
      this.loadPreviewsAndStart();
      // Start prefetching the full profile in the background
      this.workerService.prefetchProfile(this.worker.id);
    }, 300);
  }

  onMouseLeave(): void {
    this.isHovering = false;
    this.stopCarousel();
    if (this.hoverDelayId) clearTimeout(this.hoverDelayId);
    this.currentPreviewIdx = 0;
    this.cdr.markForCheck();
  }

  // ── Carousel logic ─────────────────────────────────────────────────────────

  private loadPreviewsAndStart(): void {
    if (this.previewLoaded) {
      this.startCarousel();
      return;
    }

    this.workerService.getPreviewThumbs(this.worker.id).subscribe(urls => {
      this.previewUrls  = urls;
      this.previewLoaded = true;
      this.startCarousel();
      this.cdr.markForCheck();
    });
  }

  private startCarousel(): void {
    if (this.previewUrls.length < 2) return;
    this.stopCarousel();
    this.intervalId = setInterval(() => {
      this.currentPreviewIdx = (this.currentPreviewIdx + 1) % this.previewUrls.length;
      this.cdr.markForCheck();
    }, 3000);
  }

  private stopCarousel(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
  }

  // ── Navigation ─────────────────────────────────────────────────────────────

  navigateToProfile(): void {
    this.router.navigate(['/profile'], { queryParams: { id: this.worker.id } });
  }

  // ── Active image ───────────────────────────────────────────────────────────

  get displayUrl(): string | null {
    if (this.isHovering && this.previewUrls.length > 0) {
      return this.previewUrls[this.currentPreviewIdx];
    }
    return this.worker.mainThumbUrl;
  }

  ngOnDestroy(): void {
    this.stopCarousel();
    if (this.hoverDelayId) clearTimeout(this.hoverDelayId);
  }
}
