import {Component, OnInit, ChangeDetectionStrategy} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {IonicModule} from '@ionic/angular';
import {ClientAccountService} from '../../services/client-account.service';
import {WorkerCardComponent} from '../worker-card/worker-card.component';
import {HeaderComponent} from '../header/header.component';
import {Observable} from "rxjs";
import {ClientProfile} from "../../models/user.model";
import {AccountSettingsComponent} from "../account-settings/account-settings.component";
import {GeographicZone} from "../../models/filter.model";
import {ActivatedRoute, Router} from "@angular/router";
import {tap} from "rxjs/operators";
import {ZoneSelectorComponent} from "../zone-selector/zone-selector.component";
import { addIcons } from 'ionicons';
import { heartDislikeOutline, checkmarkCircle, chatbubblesOutline, trashOutline, createOutline, openOutline } from 'ionicons/icons';
import {LanguageManagerService} from "../../services/language-manager.service";
import {settingsErrorKey} from "../../utils/error-messages";
import {AccountService} from "../../services/account-service";
import {Comment} from "../../models/items.model";

@Component({
    selector: 'app-account',
    imports: [CommonModule, FormsModule, IonicModule, WorkerCardComponent, HeaderComponent, AccountSettingsComponent],
    templateUrl: './account.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrls: ['./account.component.scss']
})
export class AccountComponent implements OnInit {
  currentUser$: Observable<ClientProfile>;
  activeTab: 'favorites' | 'settings' | 'comments' | 'account' = 'favorites';

  locations!:GeographicZone[] ;
  availableServices: string[] | undefined;
  selectedZoneId: number | undefined = undefined;
  editingCommentId: number | null = null;
  editingCommentText: string = '';
  isSavingComment: boolean = false;

  constructor(
    private clientAccountService: ClientAccountService,
    private accountService: AccountService,
    protected languageService: LanguageManagerService,
    private route : ActivatedRoute,
    private router: Router
  ) {
    addIcons({ heartDislikeOutline, checkmarkCircle,chatbubblesOutline, trashOutline, createOutline, openOutline });
    this.currentUser$ = this.clientAccountService.getCurrentAccount().pipe(
      tap(user => {
        if (user) {
          this.selectedZoneId = user.geographicZone ? user.geographicZone.id : -1;
        }
      })
    );
  }

  async ngOnInit(): Promise<void> {
    this.route.data.subscribe((data) => {
      this.availableServices = data['services'] || [];
      this.locations = data['locations'] || [];
    });
  }

  updateLocation() {
    // this.accountService.updateSettings();
  }

  onZoneChange(event: any, user: ClientProfile) {
    this.selectedZoneId = event.childZoneId;
    const targetId = event.childZoneId || event.parentZoneId;

    // Look in the root zones OR in the flattened sub-zones
    user.geographicZone = targetId
      ? (this.locations.find(p => p.id === targetId) || this.locations.flatMap(p => p.subZones || []).find(c => c.id === targetId) || null)
      : null;

    this.updateLocation();
  }

  async handleSettingsSave(event: any) {
    const { payload, setLoading, setSuccess, setError } = event;

    try {
      await this.accountService.updateSettings(payload);
      setSuccess(this.languageService.msg('settings_saved'));
    } catch (err: any) {
      setError(this.languageService.msg(settingsErrorKey(err)));
    } finally {
      setLoading(false);
    }
  }

  navigateToWorker(workerId: string): void {
    this.router.navigate(['/profile'], {
      queryParams: { id: workerId },
      state: { from: this.router.url }
    });
  }

  startEditComment(comment: Comment): void {
    this.editingCommentId = comment.id;
    this.editingCommentText = comment.content;
  }

  cancelEditComment(): void {
    this.editingCommentId = null;
    this.editingCommentText = '';
  }

  async saveComment(comment: Comment): Promise<void> {
    if (!this.editingCommentText.trim()) return;
    this.isSavingComment = true;

    try {
      await this.clientAccountService.postComment(comment.workerId, this.editingCommentText);

      // Local update
      comment.content = this.editingCommentText;
      comment.date = new Date().toISOString();

      this.cancelEditComment();
    } catch (err) {
      console.error('Could not update the review', err);
      alert(this.languageService.msg('comment_update_error'));
    } finally {
      this.isSavingComment = false;
    }
  }

  async deleteComment(commentId: number, user: ClientProfile): Promise<void> {
    if (!confirm(this.languageService.msg('comment_delete_confirm'))) return;

    try {
      await this.clientAccountService.deleteComment(commentId);
      // Remove locally: synchronous update without reloading
      user.reviews = user.reviews.filter(c => c.id !== commentId);
    } catch (err) {
      console.error('Could not delete the review', err);
      alert(this.languageService.msg('comment_delete_error'));
    }
  }
}
