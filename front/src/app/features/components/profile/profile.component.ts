import {Component, CUSTOM_ELEMENTS_SCHEMA, ElementRef, OnDestroy, OnInit, ViewChild, ChangeDetectionStrategy} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {
  IonButton,
  IonContent,
  IonIcon,
  IonModal,
  IonSpinner,
  IonTextarea,
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
import {ServiceInfoComponent} from "../service-info/service-info.component";
import {ClientAccountService} from "../../services/client-account.service";
import {AuthService} from "../../services/auth.service";
import {Service} from "../../models/common.model";
import {LanguageManagerService} from "../../services/language-manager.service";
import {formatPhone, whatsappNumber} from "../../utils/phone";
import {Subscription} from "rxjs";

@Component({
    selector: 'app-profile',
    templateUrl: './profile.component.html',
    styleUrls: ['./profile.component.scss'],
    imports: [
        CommonModule, FormsModule, IonContent, IonButton, IonIcon, HeaderComponent, IonModal, IonTextarea, IonSpinner, ServiceInfoComponent,
    ],
    changeDetection: ChangeDetectionStrategy.Eager,
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
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
      this.clientAccountService.ensureLoaded();
      this.clientAccountSub = this.clientAccountService.listenToMyAccount().subscribe({
        next: (clientAccount) => {
          // Favorite state, live from the local cache
          this.isFavorite = clientAccount?.favorites?.some(f => f.id === this.worker?.id) ?? false;
        },
        error: (err) => console.error('Client favorites stream error', err)
      });
    }
  }

  // ── Gallery navigation ──────────────────────────────────────────────────────────────

  /** Avatar click: opens the gallery on the first photo, if any. */
  openFirstPhoto(): void {
    const first = this.worker?.photos?.[0];
    if (first) this.openMedia(first);
  }

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

    // Photos and videos in one list
    this.modalMedia = [...photoItems, ...videoItems];

    // Index of the clicked item in that list
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

  // Called by Swiper on every slide change
  onSlideChange() {
    const swiperEl = this.swiperRef?.nativeElement;
    if (swiperEl && swiperEl.shadowRoot) {
      // All video elements inside Swiper's shadow DOM
      const videos = swiperEl.shadowRoot.querySelectorAll('video');
      videos.forEach((video: HTMLVideoElement) => {
        video.pause();
        // Optional: rewind the video so it restarts from the beginning
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
        await this.clientAccountService.removeFavorite(this.worker.id);
      } else {
        await this.clientAccountService.addFavorite(this.worker.id);
      }
    } catch (error) {
      console.error('Could not toggle the favorite', error);
    }
  }

  toggleNotify(): void {
    if (!this.isClient) return;
    this.notifyEnabled = !this.notifyEnabled;
    // TODO: call the future NotificationService.toggle(this.worker!.id)
  }

  async report(): Promise<void> {
    const reason = prompt(this.languageService.msg('report_reason_prompt'));

    if(reason && this.worker?.id){
      await this.clientAccountService.reportAccount(reason,this.worker?.id);
      alert(this.languageService.msg('report_sent'));
    }else{
      alert(this.languageService.msg('report_reason_required'));
    }
  }

  goBack(): void {
    const state = window.history.state;
    if (state && state.from) {
      this.router.navigateByUrl(state.from); // Back to the favorites
    } else {
      this.router.navigateByUrl('/'); // Fall back to the gallery on direct access
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
        // Editing an existing comment
        this.worker.comments[existingIndex] = savedComment;
      } else {
        // New comment
        this.worker.comments.unshift(savedComment);
      }

      // Sort again and keep the comment at the top
      this.sortAndPrepareComments(currentUser?.id);

    } catch (err) {
      console.error('Could not post the review', err);
      alert(this.languageService.msg('comment_save_error'));
    } finally {
      this.isSubmittingReview = false;
    }
  }

  /**
   * Sorts the comments:
   * 1. The logged-in client's comment first.
   * 2. The others from newest to oldest.
   * 3. PRE-FILLS the text field when the client has already written a comment.
   */
  private sortAndPrepareComments(currentUserId?: string): void {
    if (!this.worker || !this.worker.comments) return;

    // Newest first
    this.worker.comments.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());

    if (currentUserId) {
      const myComment = this.worker.comments.find(c => c.authorId === currentUserId);
      if (myComment) {
        // Pre-fill the edit field with the existing review
        this.newCommentText = myComment.content;

        // Take the client's own comment out of the list and put it first
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

  protected readonly formatPhone = formatPhone;
  protected readonly whatsappNumber = whatsappNumber;
}
