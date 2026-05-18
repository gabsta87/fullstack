import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {IonicModule} from '@ionic/angular';
import {CurrentProfile, AccountService} from '../../services/account.service';
import {WorkerService} from '../../services/worker.service';
import {
  BODY_TYPE_LABELS,
  BODY_TYPES_LIST,
  REGIONS,
  WorkerSimpleProfile
} from '../../models/worker.model';
import {WorkerCardComponent} from '../worker-card/worker-card.component';
import {HeaderComponent} from '../header/header.component';
import {firstValueFrom, Observable} from "rxjs";

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule, WorkerCardComponent, HeaderComponent],
  templateUrl: './account.component.html',
  styleUrls: ['./account.component.scss']
})
export class AccountComponent implements OnInit {
  currentUser$: Observable<CurrentProfile>;
  activeTab: 'feed' | 'favorites' | 'settings' = 'favorites';

  readonly regions = REGIONS;
  readonly bodyTypesList = BODY_TYPES_LIST;
  readonly bodyTypeLabels = BODY_TYPE_LABELS;
  availableServices: string[] | undefined;

  favorites$:Observable< WorkerSimpleProfile[]>;

  selectionStates = {
    bodyType: {} as Record<string, boolean>,
    services: {} as Record<string, boolean>
  };

  constructor(
    private accountService: AccountService,
    private workerService: WorkerService,
  ) {
    this.currentUser$ = this.accountService.getCurrentProfile();

    this.favorites$ = this.accountService.getFavorites();
  }

  async ngOnInit(): Promise<void> {
    this.availableServices = await firstValueFrom(this.workerService.getWorkersServices());
  }

  setTab(tab: 'feed' | 'favorites' | 'settings') {
    this.activeTab = tab;
  }

  updateLocation() {
    // this.accountService.updateSettings();
    console.log("TODO: update location");
  }
}
