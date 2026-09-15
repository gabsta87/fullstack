import {Component, CUSTOM_ELEMENTS_SCHEMA, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {
  IonButton,
  IonButtons,
  IonChip,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonList, IonModal,
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
  personOutline,
  playCircleOutline,
  timeOutline,
  warningOutline,
  womanOutline
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
    CommonModule, FormsModule, IonContent, IonHeader, IonToolbar, IonButtons,
    IonButton, IonIcon, IonList, IonItem, IonLabel, IonChip, HeaderComponent, IonModal,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class ProfileComponent implements OnInit, OnDestroy {
  @ViewChild('swiperRef') swiperRef?: ElementRef;

  modalMedia: { type: 'photo' | 'video'; url: string }[] = [];

  worker: WorkerFullProfile | null = null;
  isFavorite     = false;
  notifyEnabled  = false;
  isClient       = false;
  isImageOpen    = false;
  selectedPhotoIndex = 0;
  private clientAccountSub?: Subscription;

  newReview = { rating: 0, text: '' };
  starRange = [1, 2, 3, 4, 5];

  constructor(private route: ActivatedRoute,
              private clientAccountService: ClientAccountService,
              private authService : AuthService,
              private router: Router
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

  // ── Gallery navigation ──────────────────────────────────────────────────────────────

  openMedia(clickedItem: PhotoItem | VideoItem) {
    if (!this.worker) return;

    const photoItems = (this.worker.photos || []).map(p => ({
      type: 'photo' as const,
      url: p.originalUrl
    }));

    const videoItems = (this.worker.videos || []).map(v => ({
      type: 'video' as const,
      url: v.url
    }));

    // On combine les deux listes
    this.modalMedia = [...photoItems, ...videoItems];

    // On cherche dynamiquement l'index de l'élément cliqué dans la liste globale
    const targetUrl = 'originalUrl' in clickedItem ? clickedItem.originalUrl : clickedItem.url;
    const globalIndex = this.modalMedia.findIndex(m => m.url === targetUrl);

    this.isImageOpen = true;

    setTimeout(() => {
      const swiperEl = this.swiperRef?.nativeElement;
      if (swiperEl && swiperEl.swiper) {
        swiperEl.swiper.update();
        swiperEl.swiper.slideToLoop(globalIndex >= 0 ? globalIndex : 0, 0);

        swiperEl.addEventListener('swiperslidechange', () => {
          this.onSlideChange();
        });
      }
    }, 100);
  }

  closeFullSizeImage() {
    this.isImageOpen = false;
  }

  slideNext() {
    if (this.swiperRef && this.swiperRef.nativeElement.swiper) {
      this.swiperRef.nativeElement.swiper.slideNext(300);
    }
  }

  slidePrev() {
    if (this.swiperRef && this.swiperRef.nativeElement.swiper) {
      this.swiperRef.nativeElement.swiper.slidePrev(300);
    }
  }

  // Méthode appelée à chaque changement de diapositive par Swiper
  onSlideChange() {
    const swiperEl = this.swiperRef?.nativeElement;
    if (swiperEl && swiperEl.shadowRoot) {
      // Sélectionne toutes les balises vidéo à l'intérieur du Shadow DOM de Swiper
      const videos = swiperEl.shadowRoot.querySelectorAll('video');
      videos.forEach((video: HTMLVideoElement) => {
        video.pause();
        // Optionnel : remet la vidéo au début si tu veux qu'elle se relise depuis le début plus tard
        // video.currentTime = 0;
      });
    }
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

  goBack(): void {
    const state = window.history.state;
    if (state && state.from) {
      this.router.navigateByUrl(state.from); // Retourne aux favoris
    } else {
      this.router.navigateByUrl('/'); // Fallback sur la galerie si accès direct
    }
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
}
