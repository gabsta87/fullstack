import { Component, OnInit } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AlertController, IonicModule } from '@ionic/angular';
import { TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { TagModule } from 'primeng/tag';
import { AdminService } from '../../services/admin-service';
import { addIcons } from "ionicons";
import { addOutline, trashOutline, pencilOutline } from "ionicons/icons";
import { ActivatedRoute } from "@angular/router";
import { BehaviorSubject, firstValueFrom, map, Observable, switchMap } from "rxjs";
import { Service } from "../../models/common.model";
import { GeographicZoneWithParent } from "../../models/filter.model";
import { CommonService } from "../../services/common-service";

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    TableModule,
    InputTextModule,
    DropdownModule,
    TagModule
  ],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {
  currentTab: 'users' | 'certifications' | 'services' | 'zones' | 'logs' = 'users';

  // Flux d'observables réactifs principaux
  users$!: Observable<any[]>;
  pendingWorkers$!: Observable<any[]>; // Flux dérivé des utilisateurs en attente de certif
  services$!: Observable<Service[]>;
  zones$!: Observable<GeographicZoneWithParent[]>;
  auditLogs$!: Observable<any[]>;

  // Déclencheurs de rechargement automatiques pour les flux dynamiques
  private servicesRefresh$ = new BehaviorSubject<void>(undefined);
  private zonesRefresh$ = new BehaviorSubject<void>(undefined);

  flatZones$!: Observable<{ id: number; name: string; level: number; parentId: number | null }[]>;
  selectedRole: string | null = null;

  roleOptions = [
    { label: 'Tous les rôles', value: null },
    { label: 'Administrateur', value: 'ADMIN' },
    { label: 'Client', value: 'CLIENT' },
    { label: 'Annonceur', value: 'WORKER' }
  ];

  constructor(
    private adminService: AdminService,
    private commonService: CommonService,
    private alertCtrl: AlertController,
    private location: Location,
    private route: ActivatedRoute
  ) {
    addIcons({ addOutline, trashOutline, pencilOutline });
  }

  ngOnInit() {
    // 1. Récupération des données depuis les Resolvers de la route
    this.users$ = this.route.data.pipe(map(data => data['users'] || []));
    this.auditLogs$ = this.route.data.pipe(map(data => data['logs'] || []));

    // 2. Dérivation directe des workers en attente à partir du flux `users$` existant
    // (Filtre sur le rôle WORKER et le statut de certification PENDING_APPROVAL)
    this.pendingWorkers$ = this.users$.pipe(
      map(users => users
        .filter(u => u.role === 'WORKER' && u.certificationStatus === 'PENDING_APPROVAL')
        .sort((a, b) => new Date(a.certifiedAt || 0).getTime() - new Date(b.certifiedAt || 0).getTime())
      )
    );

    // 3. Flux dynamiques gérés par réactivité
    this.services$ = this.servicesRefresh$.pipe(
      switchMap(() => this.commonService.getWorkersServices())
    );

    this.zones$ = this.zonesRefresh$.pipe(
      switchMap(() => this.commonService.getGeographicZones() ? this.commonService.getGeographicZones() : this.route.data.pipe(map(data => data['zones'] || [])))
    );

    // Aplatissement intelligent pour restaurer l'indentation et le parentId
    this.flatZones$ = this.zones$.pipe(
      map(zones => {
        if (!zones) return [];
        const parents = zones.filter(z => !z.parentId);
        const result: { id: number; name: string; level: number; parentId: number | null }[] = [];

        parents.forEach(parent => {
          // Ajout du parent (parentId vaut null ou 0)
          result.push({ id: parent.id, name: parent.name, level: 0, parentId: parent.parentId ?? null });

          // Ajout des enfants directs de ce parent
          const children = zones.filter(z => z.parentId === parent.id);
          children.forEach(child => {
            result.push({ id: child.id, name: child.name, level: 1, parentId: parent.id });
          });
        });

        return result;
      })
    );
  }

  goBack() {
    this.location.back();
  }

  setLocked(user: any, lockState: boolean) {
    this.adminService.updateWorkerStatus(user.id, { locked: lockState }).subscribe({
      next: (updatedWorker) => {
        user.locked = updatedWorker.locked;
      },
      error: (err) => console.error('Échec de la modification du verrouillage', err)
    });
  }

  async promptUpdateDays(user: any) {
    const alert = await this.alertCtrl.create({
      header: `Ajuster l'abonnement de ${user.username}`,
      inputs: [
        { name: 'days', type: 'number', value: user.remainingDaysCredit, placeholder: 'Nombre de jours' },
        { name: 'reason', type: 'text', placeholder: 'Motif de la modification (Requis)' }
      ],
      buttons: [
        { text: 'Annuler', role: 'cancel' },
        {
          text: 'Enregistrer',
          handler: (data) => {
            if (!data.reason || data.reason.trim() === '') return false;

            this.adminService.updateDaysCredit(user.id, parseInt(data.days, 10), data.reason).subscribe({
              next: (res: any) => {
                user.remainingDaysCredit = res.newDaysValue;
              },
              error: (err) => console.error('Échec mise à jour crédit jours', err)
            });
            return true;
          }
        }
      ]
    });
    await alert.present();
  }

  async verifyCertif(workerId: string, approved: boolean) {
    let reason = '';

    if (!approved) {
      const alertReason = await this.alertCtrl.create({
        header: 'Motif du refus',
        inputs: [{ name: 'reason', type: 'text', placeholder: 'Ex: Panneau ou code illisible' }],
        buttons: [
          { text: 'Annuler', role: 'cancel' },
          { text: 'Valider', handler: (data) => { reason = data.reason; } }
        ]
      });
      await alertReason.present();
      const result = await alertReason.onDidDismiss();
      if (result.role === 'cancel') return;
    }

    this.adminService.verifyCertification(workerId, approved, reason).subscribe({
      next: () => {
        // Optionnel : tu pourrais émettre un rafraîchissement sur les workers si besoin
      },
      error: (err) => console.error('Échec traitement certification', err)
    });
  }

  // ── GESTION DES SERVICES ──────────────────────────────────────────────────

  async onRowEditSave(service: any) {
    if (!service.name || service.name.trim() === '') return;

    try {
      await firstValueFrom(this.adminService.updateService({
        id: service.id,
        name: service.name.trim(),
        description: service.description ? service.description.trim() : undefined
      }));
      this.servicesRefresh$.next();
    } catch (error) {
      console.error("Échec de la mise à jour du service", error);
    }
  }

  async openServiceModal() {
    const name = prompt("Nom du nouveau service :");
    if (!name) return;
    const rawDesc = prompt("Description (optionnelle) :");
    const description = rawDesc && rawDesc.trim() !== '' ? rawDesc.trim() : undefined;

    try {
      await firstValueFrom(this.adminService.updateService({ id: 0, name, description }));
      this.servicesRefresh$.next(); // Actualise le flux services$
    } catch (err: any) {
      alert("Erreur : " + (err.error || "Impossible de créer le service"));
    }
  }

  async deleteService(id: number) {
    if (confirm('Voulez-vous vraiment supprimer ce service ?')) {
      try {
        await firstValueFrom(this.adminService.deleteService(id));
        this.servicesRefresh$.next(); // Actualise le flux services$
      } catch (err) {
        alert("Erreur lors de la suppression du service");
      }
    }
  }

  // ── GESTION DES ZONES GÉOGRAPHIQUES ──────────────────────────────────────
  async openZoneModal(parentId: number | null = null) {
    const title = parentId ? "Nom de la nouvelle sous-zone :" : "Nom de la nouvelle zone parente :";
    const name = prompt(title);
    if (!name || name.trim() === '') return;

    try {
      const payload = {
        name: name.trim(),
        ...(parentId !== null ? { parentId } : {}) // N'ajoute parentId que s'il n'est pas null
      };

      await firstValueFrom(this.adminService.updateRegion(payload));
      this.zonesRefresh$.next();
    } catch (err: any) {
      alert(err.error?.error || "Erreur lors de la création de la zone");
    }
  }

  // Ouvre une modale pour modifier le nom d'une zone existante
  async editZoneName(zone: any) {
    const newName = prompt("Modifier le nom de la zone :", zone.name);
    if (!newName || newName.trim() === '' || newName === zone.name) return;

    try {
      await firstValueFrom(this.adminService.updateRegion({
        id: zone.id,
        name: newName.trim(),
        parentId: zone.parentId
      }));
      this.zonesRefresh$.next();
    } catch (err: any) {
      alert(err.error?.error || "Erreur lors de la modification de la zone");
    }
  }

  async deleteZone(id: number) {
    if (confirm('Voulez-vous vraiment supprimer cette zone ?')) {
      try {
        await firstValueFrom(this.adminService.deleteRegion(id));
        this.zonesRefresh$.next();
      } catch (err: any) {
        alert(err.error?.error || "Impossible de supprimer cette zone.");
      }
    }
  }
}
