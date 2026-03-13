// src/app/features/components/profile/profile.component.ts

import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  IonContent, IonHeader, IonToolbar, IonButtons, IonBackButton,
  IonButton, IonIcon, IonSpinner,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  callOutline, logoWhatsapp, heartOutline, heart,
  notificationsOutline, notifications, warningOutline,
  chevronBackOutline, chevronForwardOutline, closeOutline,
  playCircleOutline, locationOutline, calendarOutline,
  bodyOutline, timeOutline,
} from 'ionicons/icons';

import { WorkerProfile, PhotoItem, VideoItem, Review } from '../../models/worker.model';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss'],
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink, DatePipe,
    IonContent, IonHeader, IonToolbar, IonButtons, IonBackButton,
    IonButton, IonIcon, IonSpinner,
  ],
})
export class ProfileComponent implements OnInit, OnDestroy {

  worker: WorkerProfile | null = null;
  isLoading = false; // resolver already waited — no spinner needed on normal load

  isLoggedIn     = true;   // TODO: inject AuthService
  isFavorite     = false;
  notifyEnabled  = false;

  lightbox: {
    open: boolean; type: 'photo' | 'video';
    src: string; index: number;
    items: (PhotoItem | VideoItem)[];
  } = { open: false, type: 'photo', src: '', index: 0, items: [] };

  newReview = { rating: 0, text: '' };
  starRange = [1, 2, 3, 4, 5];

  constructor(private route: ActivatedRoute) {
    addIcons({
      callOutline, logoWhatsapp, heartOutline, heart,
      notificationsOutline, notifications, warningOutline,
      chevronBackOutline, chevronForwardOutline, closeOutline,
      playCircleOutline, locationOutline, calendarOutline,
      bodyOutline, timeOutline,
    });
  }

  ngOnInit(): void {
    // The resolver already fetched (or cache-hit) the profile —
    // it's available synchronously here, no subscribe needed.
    this.worker = this.route.snapshot.data['profile'] ?? null;
  }

  // ── Lightbox ──────────────────────────────────────────────────────────────

  openPhoto(index: number): void {
    if (!this.worker) return;
    this.lightbox = {
      open: true, type: 'photo',
      src: this.worker.photos[index].originalUrl,
      index, items: this.worker.photos,
    };
    document.body.style.overflow = 'hidden';
  }

  openVideo(index: number): void {
    if (!this.worker) return;
    this.lightbox = {
      open: true, type: 'video',
      src: this.worker.videos[index].url,
      index, items: this.worker.videos,
    };
    document.body.style.overflow = 'hidden';
  }

  closeLightbox(): void {
    this.lightbox.open = false;
    document.body.style.overflow = '';
  }

  lbPrev(e: Event): void {
    e.stopPropagation();
    const i = (this.lightbox.index - 1 + this.lightbox.items.length) % this.lightbox.items.length;
    this.lightbox.index = i;
    this.lightbox.src   = this.lightbox.type === 'photo'
      ? (this.lightbox.items[i] as PhotoItem).originalUrl
      : (this.lightbox.items[i] as VideoItem).url;
  }

  lbNext(e: Event): void {
    e.stopPropagation();
    const i = (this.lightbox.index + 1) % this.lightbox.items.length;
    this.lightbox.index = i;
    this.lightbox.src   = this.lightbox.type === 'photo'
      ? (this.lightbox.items[i] as PhotoItem).originalUrl
      : (this.lightbox.items[i] as VideoItem).url;
  }

  // ── User actions ──────────────────────────────────────────────────────────

  toggleFavorite(): void {
    this.isFavorite = !this.isFavorite;
    // TODO: FavoritesService.toggle(this.worker!.id)
  }

  toggleNotify(): void {
    this.notifyEnabled = !this.notifyEnabled;
    // TODO: NotificationService.toggle(this.worker!.id)
  }

  report(): void {
    alert('Signalement envoyé.');
    // TODO: ReportService.report(this.worker!.id)
  }

  // ── Reviews ───────────────────────────────────────────────────────────────

  submitReview(): void {
    if (!this.worker || !this.newReview.text || !this.newReview.rating) return;
    const r: Review = {
      author: 'Vous', authorInitial: 'V',
      rating: this.newReview.rating,
      date:   new Date().toISOString(),
      text:   this.newReview.text,
    };
    this.worker.reviews.unshift(r);
    this.worker.reviewCount++;
    this.newReview = { rating: 0, text: '' };
    // TODO: ReviewService.post(this.worker.id, r)
  }

  ratingFill(s: number, rating: number): string {
    return s <= rating ? '#c8956c' : '#e8e4df';
  }

  ngOnDestroy(): void {
    document.body.style.overflow = '';
  }
}
