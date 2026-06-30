import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonChip,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonToolbar,
} from '@ionic/angular/standalone';
import {addIcons} from 'ionicons';
import {
  bodyOutline,
  calendarOutline,
  callOutline,
  chevronBackOutline,
  chevronForwardOutline,
  closeOutline,
  heart,
  heartOutline,
  locationOutline,
  logoWhatsapp,
  notifications,
  notificationsOutline,
  playCircleOutline,
  timeOutline,
  warningOutline,
  womanOutline,
  personOutline
} from 'ionicons/icons';

import {PhotoItem, Review, VideoItem} from '../../models/items.model';
import {WorkerFullProfile} from "../../models/user.model";
import {HeaderComponent} from "../header/header.component";
import {ClientAccountService} from "../../services/client-account.service";
import {AuthService} from "../../services/auth.service";
import {Subscription} from "rxjs";

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss'],
  standalone: true,
  imports: [
    CommonModule, FormsModule, IonContent, IonHeader, IonToolbar, IonButtons, IonBackButton,
    IonButton, IonIcon, IonList, IonItem, IonLabel, IonChip, HeaderComponent,
  ],
})
export class ProfileComponent implements OnInit, OnDestroy {

  worker: WorkerFullProfile | null = null;
  isFavorite     = false;
  notifyEnabled  = false;
  isClient       = false;
  private clientAccountSub?: Subscription;

  lightbox: {
    open: boolean; type: 'photo' | 'video';
    src: string; index: number;
    items: (PhotoItem | VideoItem)[];
  } = { open: false, type: 'photo', src: '', index: 0, items: [] };

  newReview = { rating: 0, text: '' };
  starRange = [1, 2, 3, 4, 5];

  constructor(private route: ActivatedRoute,
              private clientAccountService: ClientAccountService,
              private authService : AuthService
  ) {
    addIcons({
      callOutline, logoWhatsapp, heartOutline, heart,
      notificationsOutline, notifications, warningOutline,
      chevronBackOutline, chevronForwardOutline, closeOutline,
      playCircleOutline, locationOutline, calendarOutline,
      bodyOutline, timeOutline, womanOutline, personOutline
    });
  }

  ngOnInit(): void {
    // The resolver already fetched (or cache-hit) the profile —
    // it's available synchronously here, no subscribe needed.
    this.worker = this.route.snapshot.data['profile'] ?? null;

    const currentUser = this.authService.getUser();
    this.isClient = currentUser?.role === 'CLIENT';

    if (this.isClient && this.worker) {
      this.clientAccountSub = this.clientAccountService.listenToMyAccount().subscribe({
        next: (clientAccount) => {
          // On vérifie en temps réel si l'ID du worker est dans la liste des favoris du client
          this.isFavorite = clientAccount?.favorites?.some(f => f.id === this.worker?.id) ?? false;
          console.log(`[SSE] Statut favori mis à jour pour ${this.worker?.username} :`, this.isFavorite);
        },
        error: (err) => console.error("Erreur du flux de favoris client", err)
      });
    }
    console.log("Worker : ", this.worker);
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

  async toggleFavorite(): Promise<void> {
    if (!this.worker || !this.isClient) return;

    try {
      if (this.isFavorite) {
        console.log("Retrait des favoris...");
        await this.clientAccountService.removeFavorite(this.worker.id);
      } else {
        console.log("Ajout aux favoris...");
        await this.clientAccountService.addFavorite(this.worker.id);
      }
      // Note : Pas besoin d'écrire "this.isFavorite = !this.isFavorite" manuellement ici !
      // Dès que le serveur répond, le flux SSE (Etape 2) va intercepter l'événement
      // et faire basculer l'icône automatiquement à l'écran.
    } catch (error) {
      console.error("Impossible de modifier le favori :", error);
    }
  }

  toggleNotify(): void {
    if (!this.isClient) return;
    this.notifyEnabled = !this.notifyEnabled;
    // TODO: Appel à ton futur NotificationService.toggle(this.worker!.id)
  }

  report(): void {
    alert('Signalement envoyé.');
    // TODO: ReportService.report(this.worker!.id)
  }

  // ── Reviews ───────────────────────────────────────────────────────────────

  submitReview(): void {
    if (!this.worker || !this.newReview.text || !this.newReview.rating) return;
    const r: Review = {
      author: 'Vous',
      date:   new Date().toISOString(),
      text:   this.newReview.text,
    };
    this.worker.reviews.unshift(r);
    this.newReview = { rating: 0, text: '' };
    // TODO: ReviewService.post(this.worker.id, r)
  }

  ratingFill(s: number, rating: number): string {
    return s <= rating ? '#c8956c' : '#e8e4df';
  }

  ngOnDestroy(): void {
    document.body.style.overflow = '';
  }

  addFavorite() : void{
    if(this.worker == null) return;
    this.clientAccountService.addFavorite(this.worker?.id);
  }
}
