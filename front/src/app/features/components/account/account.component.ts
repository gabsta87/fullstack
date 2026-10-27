import {Component, OnInit} from '@angular/core';
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
import {AccountService} from "../../services/account-service";
import {Comment} from "../../models/items.model";

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule, WorkerCardComponent, HeaderComponent, AccountSettingsComponent],
  templateUrl: './account.component.html',
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
    console.log("TODO: update location");
  }

  onZoneChange(event: any, user: ClientProfile) {
    this.selectedZoneId = event.childZoneId;
    const targetId = event.childZoneId || event.parentZoneId;

    // On cherche dans les parents OU dans les sous-zones aplaties en une seule ligne
    user.geographicZone = targetId
      ? (this.locations.find(p => p.id === targetId) || this.locations.flatMap(p => p.subZones || []).find(c => c.id === targetId) || null)
      : null;

    this.updateLocation();
  }

  async handleSettingsSave(event: any) {
    const { payload, setLoading, setSuccess, setError } = event;

    try {
      await this.accountService.updateSettings(payload);
      setSuccess('Account updated successfully');
    } catch (err: any) {
      setError(err?.error?.message || 'An error occured while updating');
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

      // Mise à jour locale
      comment.content = this.editingCommentText;
      comment.date = new Date().toISOString();

      this.cancelEditComment();
    } catch (err) {
      console.error("Erreur lors de la modification du commentaire", err);
      alert("Erreur lors de la mise à jour.");
    } finally {
      this.isSavingComment = false;
    }
  }

  async deleteComment(commentId: number, user: ClientProfile): Promise<void> {
    if (!confirm("Voulez-vous vraiment supprimer ce commentaire ?")) return;

    try {
      await this.clientAccountService.deleteComment(commentId);
      // Retrait local pour mise à jour synchrone sans rechargement
      user.reviews = user.reviews.filter(c => c.id !== commentId);
    } catch (err) {
      console.error("Erreur lors de la suppression", err);
      alert("Impossible de supprimer le commentaire.");
    }
  }
}
