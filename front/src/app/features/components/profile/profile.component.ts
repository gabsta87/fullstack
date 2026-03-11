// import {Component, OnInit} from '@angular/core';
// import {NgForOf, NgOptimizedImage} from "@angular/common";
// import {IonicModule} from "@ionic/angular";
// import { CommonModule } from '@angular/common';
// import {UploadImageComponent} from "../upload-image/upload-image.component";
// import {ActivatedRoute} from "@angular/router";
//
// @Component({
//   selector: 'app-profile',
//   standalone: true,
//   imports: [
//     NgForOf,
//     NgOptimizedImage,
//     IonicModule,
//     CommonModule,
//     UploadImageComponent,
//   ],
//   templateUrl: './profile.component.html',
//   styleUrl: './profile.component.scss'
// })
// export class ProfileComponent implements OnInit {
//
//   profileSimple !: ProfileDetail;
//
//   constructor(private route: ActivatedRoute) {}
//
//   ngOnInit(): void {
//     this.profileSimple = this.route.snapshot.data['profile'];
//     console.log(this.profileSimple);
//   }
// }
//
// // This class is used in the gallery, to have minimal information on all profiles
// export class Profile{
//   id!: number;
//   pseudo!:string;
//   description!:string;
//   phone!:string;
//   address!:string;
//   comments!:string[];
//   mainPhoto!:string;
// }
//
// // This class is used for a profile detailed page, with all photos loaded
// export class ProfileDetail extends Profile{
//   photos!:string[];
// }

// src/app/features/profile/profile.component.ts

import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  IonContent, IonHeader, IonToolbar, IonButtons, IonBackButton,
  IonButton, IonIcon, IonModal, IonSpinner,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  callOutline, logoWhatsapp, heartOutline, heart,
  notificationsOutline, notifications, warningOutline,
  chevronBackOutline, chevronForwardOutline, closeOutline,
  playCircleOutline,
} from 'ionicons/icons';

import { WorkerProfile, PhotoItem, VideoItem, Review } from '../../models/worker.model';
import { WorkerService } from '../../services/worker.service';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss'],
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink, DatePipe,
    IonContent, IonHeader, IonToolbar, IonButtons, IonBackButton,
    IonButton, IonIcon, IonModal, IonSpinner,
  ],
})

export class ProfileComponent implements OnInit, OnDestroy {

  worker: WorkerProfile | null = null;
  isLoading = true;

  // Auth (replace with real AuthService)
  isLoggedIn    = true;
  isFavorite    = false;
  notifyEnabled = false;

  // Lightbox
  lightbox: {
    open: boolean;
    type: 'photo' | 'video';
    src: string;
    index: number;
    items: (PhotoItem | VideoItem)[];
  } = { open: false, type: 'photo', src: '', index: 0, items: [] };

  // New review
  newReview = { rating: 0, text: '' };

  constructor(
    private route: ActivatedRoute,
    private workerService: WorkerService,
  ) {
    addIcons({
      callOutline, logoWhatsapp, heartOutline, heart,
      notificationsOutline, notifications, warningOutline,
      chevronBackOutline, chevronForwardOutline, closeOutline,
      playCircleOutline,
    });
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.queryParamMap.get('id'));
    if (!id) return;

    // Use prefetched cache if available (worker hovered the card)
    const cached = this.workerService.getCachedProfile(id);
    if (cached) {
      this.worker    = cached;
      this.isLoading = false;
    } else {
      this.workerService.getProfile(id).subscribe({
        next:  w  => { this.worker = w; this.isLoading = false; },
        error: () => { this.isLoading = false; },
      });
    }
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
    const item = this.lightbox.items[i];
    this.lightbox.src = this.lightbox.type === 'photo'
      ? (item as PhotoItem).originalUrl
      : (item as VideoItem).url;
  }

  lbNext(e: Event): void {
    e.stopPropagation();
    const i = (this.lightbox.index + 1) % this.lightbox.items.length;
    this.lightbox.index = i;
    const item = this.lightbox.items[i];
    this.lightbox.src = this.lightbox.type === 'photo'
      ? (item as PhotoItem).originalUrl
      : (item as VideoItem).url;
  }

  // ── Actions ───────────────────────────────────────────────────────────────

  toggleFavorite(): void {
    this.isFavorite = !this.isFavorite;
    // TODO: FavoritesService.toggle(this.worker.id)
  }

  toggleNotify(): void {
    this.notifyEnabled = !this.notifyEnabled;
    // TODO: NotificationService.toggle(this.worker.id)
  }

  report(): void {
    // TODO: open report modal / call ReportService
    alert('Signalement envoyé.');
  }

  // ── Reviews ───────────────────────────────────────────────────────────────

  submitReview(): void {
    if (!this.worker || !this.newReview.text || !this.newReview.rating) return;
    const review: Review = {
      author:        'Vous',
      authorInitial: 'V',
      rating:        this.newReview.rating,
      date:          new Date().toISOString(),
      text:          this.newReview.text,
    };
    this.worker.reviews.unshift(review);
    this.worker.reviewCount++;
    this.newReview = { rating: 0, text: '' };
    // TODO: ReviewService.post(this.worker.id, review)
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  starRange = [1, 2, 3, 4, 5];

  ratingFill(s: number, rating: number): string {
    return s <= rating ? '#c8956c' : '#e8e4df';
  }

  ngOnDestroy(): void {
    document.body.style.overflow = '';
  }
}
