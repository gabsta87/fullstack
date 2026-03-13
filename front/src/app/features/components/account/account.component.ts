import { Component, OnInit } from '@angular/core';
import { AccountService, AccountMe, SettingsUpdate } from '../../services/account.service';
import { WorkerGalleryDTO } from '../../models/worker.model';
import {CommonModule} from "@angular/common";
import {IonicModule} from "@ionic/angular";
import {FormsModule} from "@angular/forms";

@Component({
  selector: 'app-account',
  templateUrl: './account.component.html',
  styleUrls: ['./account.component.scss'],
  imports: [CommonModule, FormsModule, IonicModule],
  standalone: true
})
export class AccountComponent implements OnInit {

  me: AccountMe | null = null;
  favorites: WorkerGalleryDTO[] = [];
  activeTab: 'favorites' | 'settings' = 'favorites';

  // Settings form
  settingsForm: SettingsUpdate = {};
  settingsPassword2 = '';
  settingsSaved = false;
  settingsError = '';
  savingSettings = false;

  loading = true;
  favoritesLoading = true;

  constructor(private accountService: AccountService) {}

  ngOnInit(): void {
    this.accountService.getMe().subscribe({
      next: me => {
        this.me = me;
        this.settingsForm = { username: me.username, email: me.email };
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });

    this.accountService.getFavorites().subscribe({
      next: favs => {
        this.favorites = favs;
        this.favoritesLoading = false;
      },
      error: () => { this.favoritesLoading = false; }
    });
  }

  setTab(tab: 'favorites' | 'settings') {
    this.activeTab = tab;
    this.settingsSaved = false;
    this.settingsError = '';
  }

  removeFavorite(workerId: string) {
    this.accountService.removeFavorite(workerId).subscribe(() => {
      this.favorites = this.favorites.filter(f => f.id !== workerId);
    });
  }

  saveSettings() {
    this.settingsError = '';
    this.settingsSaved = false;

    if (this.settingsForm.password && this.settingsForm.password !== this.settingsPassword2) {
      this.settingsError = 'Passwords do not match.';
      return;
    }

    const payload: SettingsUpdate = {};
    if (this.settingsForm.username) payload.username = this.settingsForm.username;
    if (this.settingsForm.email)    payload.email    = this.settingsForm.email;
    if (this.settingsForm.password) payload.password = this.settingsForm.password;

    this.savingSettings = true;
    this.accountService.updateSettings(payload).subscribe({
      next: () => {
        this.settingsSaved = true;
        this.savingSettings = false;
        this.settingsForm.password = '';
        this.settingsPassword2 = '';
        if (this.me) {
          this.me.username = payload.username ?? this.me.username;
          this.me.email    = payload.email    ?? this.me.email;
        }
      },
      error: () => {
        this.settingsError = 'Failed to save settings.';
        this.savingSettings = false;
      }
    });
  }
}
