import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {IonicModule} from '@ionic/angular';
import {ClientAccountService} from '../../services/client-account.service';
import {WorkerService} from '../../services/worker.service';
import {BODY_TYPE_LABELS, BODY_TYPES_LIST,} from '../../models/items.model';
import {WorkerCardComponent} from '../worker-card/worker-card.component';
import {HeaderComponent} from '../header/header.component';
import {Observable} from "rxjs";
import {ClientPrivateAccount} from "../../models/user.model";
import {AccountSettingsComponent} from "../account-settings/account-settings.component";
import {GeographicZone} from "../../models/filter.model";
import {ActivatedRoute} from "@angular/router";
import {tap} from "rxjs/operators";
import {ZoneSelectorComponent} from "../zone-selector/zone-selector.component";

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule, WorkerCardComponent, HeaderComponent, AccountSettingsComponent, ZoneSelectorComponent],
  templateUrl: './account.component.html',
  styleUrls: ['./account.component.scss']
})
export class AccountComponent implements OnInit {
  currentUser$: Observable<ClientPrivateAccount>;
  activeTab: 'favorites' | 'settings' | 'account' = 'favorites';

  locations!:GeographicZone[] ;
  readonly bodyTypesList = BODY_TYPES_LIST;
  readonly bodyTypeLabels = BODY_TYPE_LABELS;
  availableServices: string[] | undefined;
  selectedZoneId: number | undefined = undefined;

  selectionStates = {
    bodyType: {} as Record<string, boolean>,
    services: {} as Record<string, boolean>
  };

  constructor(
    private accountService: ClientAccountService,
    private workerService: WorkerService,
    private route : ActivatedRoute,
  ) {
    this.currentUser$ = this.accountService.getCurrentAccount().pipe(
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

  setTab(tab: 'account' | 'favorites' | 'settings') {
    this.activeTab = tab;
  }

  updateLocation() {
    // this.accountService.updateSettings();
    console.log("TODO: update location");
  }

  onZoneChange(event: any, user: ClientPrivateAccount) {
    const value = event.detail.value;
    this.selectedZoneId = value;

    if (value === -1) {
      user.geographicZone = null;
    } else {
      const selectedRegion = this.locations.find(r => r.id === value);
      user.geographicZone = selectedRegion || null;
    }

    this.updateLocation();
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
