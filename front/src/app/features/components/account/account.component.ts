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
import {AccountSettingsComponent} from "../account-settings/account-settings.component";
import {GeographicZone} from "../../models/filter.model";

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule, WorkerCardComponent, HeaderComponent, AccountSettingsComponent],
  templateUrl: './account.component.html',
  styleUrls: ['./account.component.scss']
})
export class AccountComponent implements OnInit {
  currentUser$: Observable<ClientPrivateAccount>;
  activeTab: 'favorites' | 'settings' | 'account' = 'favorites';

  regions!:GeographicZone[] ;
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
    this.regions = await this.workerService.getGeographicZones();
  }

  setTab(tab: 'account' | 'favorites' | 'settings') {
    this.activeTab = tab;
  }

  updateLocation() {
    // this.accountService.updateSettings();
    console.log("TODO: update location");
  }

  async handleSettingsSave(event: any) {
    const { payload, setLoading, setSuccess, setError } = event;

    try {
      // Appel de votre service API existant pour modifier les identifiants
      await this.accountService.updateSettings(payload); // Ajustez selon votre nom de méthode de service actuel
      setSuccess('Vos paramètres de compte ont été mis à jour avec succès.');
    } catch (err: any) {
      setError(err?.error?.message || 'Une erreur est survenue lors de la mise à jour.');
    } finally {
      setLoading(false);
    }
  }
}
