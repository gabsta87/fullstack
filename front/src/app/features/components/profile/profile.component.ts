import {Component, CUSTOM_ELEMENTS_SCHEMA, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {
  IonBadge,
  IonButton,
  IonButtons,
  IonChip,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonModal,
  IonSpinner,
  IonTextarea,
  IonToolbar,
} from '@ionic/angular/standalone';
import {addIcons} from 'ionicons';
import {
  alertCircleOutline,
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
import {Comment, PhotoItem, VideoItem} from '../../models/items.model';
import {WorkerPublicFullProfile} from "../../models/user.model";
import {HeaderComponent} from "../header/header.component";
import {ClientAccountService} from "../../services/client-account.service";
import {AuthService} from "../../services/auth.service";
import {Service} from "../../models/common.model";
import {LanguageManagerService} from "../../services/language-manager.service";
import {Subscription} from "rxjs";

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss'],
  standalone: true,
  imports: [
    CommonModule, FormsModule, IonContent, IonHeader, IonToolbar, IonButtons,
    IonButton, IonIcon, IonList, IonItem, IonLabel, IonChip, HeaderComponent, IonModal, IonBadge, IonTextarea, IonSpinner,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class ProfileComponent implements OnInit, OnDestroy {
  @ViewChild('swiperRef') swiperRef?: ElementRef;

  services! : Service[];

  workerServices: Service[] = [];

  modalMedia: { type: 'photo' | 'video'; url: string }[] = [];

  worker: WorkerPublicFullProfile | null = null;
  isFavorite     = false;
  notifyEnabled  = false;
  isClient       = false;
  isImageOpen    = false;
  isContactOpen  = false;

  private clientAccountSub?: Subscription;

  newCommentText: string = '';
  isSubmittingReview: boolean = false;

  constructor(private route: ActivatedRoute,
              private clientAccountService: ClientAccountService,
              protected authService : AuthService,
              private router: Router,
              protected languageService : LanguageManagerService
  ) {
    addIcons({
      callOutline, logoWhatsapp, heartOutline, heart,
      notificationsOutline, notifications, warningOutline,
      chevronBackOutline, chevronForwardOutline, closeOutline,
      playCircleOutline, locationOutline, calendarOutline,
      bodyOutline, timeOutline, womanOutline, personOutline,
      alertCircleOutline
    });
  }

  ngOnInit(): void {
    this.worker = this.route.snapshot.data['profile'] ?? null;
    this.services = this.route.snapshot.data['services'] ?? [];

    if (this.worker && this.worker.servicesId) {
      this.workerServices = this.services.filter(s =>
        this.worker!.servicesId.includes(s.id)
      );
    }

    const currentUser = this.authService.getUser();
    this.isClient = currentUser?.role === 'CLIENT';

    if (this.worker && this.worker.comments) {
      this.sortAndPrepareComments(currentUser?.id);
    }

    if (this.isClient && this.worker) {
      this.clientAccountSub = this.clientAccountService.listenToMyAccount().subscribe({
        next: (clientAccount) => {
          // Vérification en temps réel de la liste des favoris depuis le cache local
          this.isFavorite = clientAccount?.favorites?.some(f => f.id === this.worker?.id) ?? false;
        },
        error: (err) => console.error("Erreur du flux de favoris client", err)
      });
    }
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

  openContactModal() {
    this.isContactOpen = true;
  }

  closeContactModal() {
    this.isContactOpen = false;
  }

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
    } catch (error) {
      console.error("Impossible de modifier le favori :", error);
    }
  }

  toggleNotify(): void {
    if (!this.isClient) return;
    this.notifyEnabled = !this.notifyEnabled;
    // TODO: Appel à ton futur NotificationService.toggle(this.worker!.id)
  }

  async report(): Promise<void> {
    const reason = prompt("Raison du signalement :");

    if(reason && this.worker?.id){
      await this.clientAccountService.reportAccount(reason,this.worker?.id);
      alert('Signalement envoyé.');
    }else{
      alert('Impossible de signaler sans fournir une raison valide');
    }
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

  async submitReview(): Promise<void> {
    if (!this.worker || !this.newCommentText.trim() || !this.isClient) return;

    this.isSubmittingReview = true;
    try {
      const savedComment: Comment = await this.clientAccountService.postComment(this.worker.id, this.newCommentText);

      if (!this.worker.comments) {
        this.worker.comments = [];
      }

      const currentUser = this.authService.getUser();
      const existingIndex = this.worker.comments.findIndex(c => c.authorId === currentUser?.id);

      if (existingIndex >= 0) {
        // Édition d'un commentaire existant
        this.worker.comments[existingIndex] = savedComment;
      } else {
        // Nouveau commentaire
        this.worker.comments.unshift(savedComment);
      }

      // Ré-appliquer le tri et s'assurer que le commentaire reste en haut
      this.sortAndPrepareComments(currentUser?.id);

    } catch (err) {
      console.error("Erreur lors de l'envoi du commentaire :", err);
      alert("Impossible d'enregistrer le commentaire.");
    } finally {
      this.isSubmittingReview = false;
    }
  }

  /**
   * Trie les commentaires :
   * 1. Le commentaire du client connecté tout en haut.
   * 2. Les autres commentaires triés du plus récent au plus ancien.
   * 3. PRÉ-REMPLIT le champ de texte si le client a déjà rédigé un commentaire.
   */
  private sortAndPrepareComments(currentUserId?: string): void {
    if (!this.worker || !this.worker.comments) return;

    // Tri par date décroissante (plus récent d'abord)
    this.worker.comments.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());

    if (currentUserId) {
      const myComment = this.worker.comments.find(c => c.authorId === currentUserId);
      if (myComment) {
        // Pré-remplir le champ de modification avec l'avis existant
        this.newCommentText = myComment.content;

        // Retirer son commentaire de la liste globale puis le placer tout en haut
        this.worker.comments = [
          myComment,
          ...this.worker.comments.filter(c => c.authorId !== currentUserId)
        ];
      }
    }
  }

  hasMyComment(): boolean { return this.worker?.comments?.some(c => c.authorId === this.authService.getUser()?.id) ?? false; }

  ngOnDestroy(): void {
    document.body.style.overflow = '';
  }
}
