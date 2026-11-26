import {sortByMastery} from "../../utils/languages";
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
  womanOutline,
  shieldCheckmarkOutline,
  trashOutline,
  banOutline,
  lockClosedOutline,
  lockOpenOutline
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
import {firstValueFrom, Subscription} from "rxjs";
import {AdminService} from "../../services/admin-service";
import {WorkerService} from "../../services/worker.service";
import {canManageAccount} from "../../utils/admin-rights";
import {UserRole} from "../../models/roles";

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
  protected readonly sortByMastery = sortByMastery;

  @ViewChild('swiperRef') swiperRef?: ElementRef;

  services! : Service[];

  workerServices: Service[] = [];

  modalMedia: { type: 'photo' | 'video'; url: string; thumbnailUrl?: string | null }[] = [];

  worker: WorkerPublicFullProfile | null = null;
  isFavorite     = false;
  notifyEnabled  = false;
  isClient       = false;
  isImageOpen    = false;
  isContactOpen  = false;

  private clientAccountSub?: Subscription;
  private adminSub?: Subscription;
  /** Admin visiting: moderation from the profile itself (ban, lock, delete comments), refreshed live. */
  isAdmin = false;
  adminBusy = false;
  adminError: string | null = null;

  newCommentText: string = '';
  isSubmittingReview: boolean = false;

  constructor(private route: ActivatedRoute,
              private clientAccountService: ClientAccountService,
              protected authService : AuthService,
              private router: Router,
              protected languageService : LanguageManagerService,
              private adminService: AdminService,
              private workerService: WorkerService
  ) {
    addIcons({
      callOutline, logoWhatsapp, heartOutline, heart,
      notificationsOutline, notifications, warningOutline,
      chevronBackOutline, chevronForwardOutline, closeOutline,
      playCircleOutline, locationOutline, calendarOutline,
      bodyOutline, timeOutline, womanOutline, personOutline,
      alertCircleOutline, shieldCheckmarkOutline, trashOutline, banOutline, lockClosedOutline, lockOpenOutline
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

    const role = currentUser?.role;
    this.isAdmin = !!this.worker && (role === UserRole.ADMIN || role === UserRole.SUPER_ADMIN)
      && canManageAccount(currentUser, { id: this.worker.id, role: UserRole.WORKER });
    if (this.isAdmin) {
      this.adminSub = this.adminService.workerChanged$.subscribe(id => {
        if (id === this.worker?.id) void this.reloadProfile();
      });
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
      if (!this.worker.available) {
        this.clientAccountService.getAvailabilityAlerts()
          .then(ids => this.notifyEnabled = ids.includes(this.worker!.id))
          .catch(err => console.warn('Could not load the availability alerts', err));
      }
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
      url: v.url,
      thumbnailUrl: v.thumbnailUrl
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

  /** "Notify me when back": one email when this unavailable profile is available again. */
  async toggleNotify(): Promise<void> {
    if (!this.isClient || !this.worker) return;
    const enable = !this.notifyEnabled;
    try {
      await this.clientAccountService.setAvailabilityAlert(this.worker.id, enable);
      this.notifyEnabled = enable;
      if (enable) alert(this.languageService.msg('notify_enabled'));
    } catch (err) {
      console.error('Could not change the availability alert', err);
      alert(this.languageService.msg('notify_error'));
    }
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

  // ── Admin moderation ───────────────────────────────────────────────────────

  private async reloadProfile() {
    if (!this.worker) return;
    try {
      this.worker = await firstValueFrom(this.workerService.refreshProfile(this.worker.id));
      this.workerServices = this.services.filter(s => this.worker!.servicesId?.includes(s.id));
      this.sortAndPrepareComments(this.authService.getUser()?.id);
    } catch (err) {
      console.error('Could not reload the profile', err);
    }
  }

  /** Ban / lock (or lift them). Same endpoint and rules as the dashboard; the backend checks the rights. */
  async adminSetStatus(change: { banned?: boolean; locked?: boolean }) {
    if (!this.worker || this.adminBusy) return;
    if (change.banned && !confirm(this.languageService.msg('admin_ban_confirm', { name: this.worker.username }))) return;
    this.adminBusy = true;
    this.adminError = null;
    try {
      await firstValueFrom(this.adminService.updateWorkerStatus(this.worker.id, change));
      await this.reloadProfile();
    } catch (err) {
      console.error('Status change failed', err);
      this.adminError = this.languageService.msg('admin_action_error');
    } finally {
      this.adminBusy = false;
    }
  }

  async adminDeleteComment(comment: Comment) {
    if (!confirm(this.languageService.msg('admin_comment_delete_confirm', { author: comment.authorUsername || '' }))) return;
    this.adminError = null;
    try {
      await this.adminService.deleteComment(comment.id);
      await this.reloadProfile();
    } catch (err) {
      console.error('Comment deletion failed', err);
      this.adminError = this.languageService.msg('admin_action_error');
    }
  }

  ngOnDestroy(): void {
    this.adminSub?.unsubscribe();
    this.clientAccountSub?.unsubscribe();
    document.body.style.overflow = '';
  }

  protected readonly formatPhone = formatPhone;
  protected readonly whatsappNumber = whatsappNumber;
}
