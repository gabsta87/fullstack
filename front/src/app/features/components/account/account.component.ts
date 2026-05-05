import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { AccountService, AccountMe, SettingsUpdate } from '../../services/account.service';
import { WorkerService } from '../../services/worker.service';
import { WorkerGalleryDTO, GalleryFilters } from '../../models/worker.model';
import { WorkerCardComponent } from '../worker-card/worker-card.component';
import { HeaderComponent } from '../header/header.component';
import { REGIONS, BODY_TYPES, SERVICES, EYE_COLORS, HAIR_COLORS } from '../../models/worker.model';

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule, WorkerCardComponent, HeaderComponent],
  templateUrl: './account.component.html',
  styleUrls: ['./account.component.scss']
})
export class AccountComponent implements OnInit {
  me: AccountMe | null = null;
  activeTab: 'feed' | 'favorites' | 'settings' = 'favorites';

  readonly regions = REGIONS;
  readonly bodyTypes = BODY_TYPES;
  readonly availableServices = SERVICES;

  selectionStates = {
    bodyType: {} as Record<string, boolean>,
    services: {} as Record<string, boolean>
  };

  // Données
  favorites: WorkerGalleryDTO[] = [];
  personalizedWorkers: WorkerGalleryDTO[] = [];

  // Formulaires
  settingsForm: SettingsUpdate = {};
  prefForm: any = {}; // Filtres de préférence

  loading = true;

  constructor(
    private accountService: AccountService,
    private workerService: WorkerService
  ) {}

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.accountService.getMe().subscribe(me => {
      this.me = me;
      this.settingsForm = { username: me.username, email: me.email };
      // On initialise les préférences avec les données du compte (ex: region, bodyType...)
      this.prefForm = { region: me.region, bodyType: me.bodyType };

      this.loadPersonalizedFeed();
      this.loadFavorites();
      this.loading = false;
    });
  }

  loadFavorites() {
    this.accountService.getFavorites().subscribe(favs => this.favorites = favs);
  }

  get connectedFavoritesCount(): number {
    return this.favorites.filter(favorite => favorite.available).length;
  }

  loadPersonalizedFeed() {
    const filters: GalleryFilters = {
      region: this.prefForm.region,
      bodyType: this.prefForm.bodyType
    };

    // Ajoutez ": any" ou le type spécifique de votre réponse
    this.workerService.getWorkers(0, 10, filters).subscribe((res: any) => {
      this.personalizedWorkers = res.content || res;
    });
  }

  setTab(tab: 'feed' | 'favorites' | 'settings') {
    this.activeTab = tab;
  }

  savePreferences() {
    // Ici on sauvegarde les filtres dans le profil via accountService
    // puis on recharge le flux personnalisé
    this.loadPersonalizedFeed();
  }
}
