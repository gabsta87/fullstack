import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {IonicModule} from '@ionic/angular';
import {ClientAccountService} from '../../services/client-account.service';
import {WorkerService} from '../../services/worker.service';
import {BODY_TYPE_LABELS, BODY_TYPES_LIST, REGIONS,} from '../../models/items.model';
import {WorkerCardComponent} from '../worker-card/worker-card.component';
import {HeaderComponent} from '../header/header.component';
import {firstValueFrom, Observable} from "rxjs";
import {ClientPrivateAccount} from "../../models/user.model";

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule, WorkerCardComponent, HeaderComponent],
  templateUrl: './account.component.html',
  styleUrls: ['./account.component.scss']
})
export class AccountComponent implements OnInit {
  currentUser$: Observable<ClientPrivateAccount>;
  activeTab: 'feed' | 'favorites' | 'settings' = 'favorites';

  readonly regions = REGIONS;
  readonly bodyTypesList = BODY_TYPES_LIST;
  readonly bodyTypeLabels = BODY_TYPE_LABELS;
  availableServices: string[] | undefined;

  selectionStates = {
    bodyType: {} as Record<string, boolean>,
    services: {} as Record<string, boolean>
  };

  constructor(
    private accountService: ClientAccountService,
    private workerService: WorkerService,
  ) {
    this.currentUser$ = this.accountService.getCurrentAccount();
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
