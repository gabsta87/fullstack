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
import { addOutline, trashOutline } from "ionicons/icons";
import {ActivatedRoute} from "@angular/router";

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

  users: any[] = [];
  workers: any[] = [];
  services: any[] = [];
  zones: any[] = [];
  auditLogs: any[] = [];

  selectedRole: string | null = null;
  pendingCertificationsCount = 0;

  roleOptions = [
    { label: 'Tous les rôles', value: null },
    { label: 'Administrateur', value: 'ADMIN' },
    { label: 'Client', value: 'CLIENT' },
    { label: 'Annonceur', value: 'WORKER' }
  ];

  constructor(
    private adminService: AdminService,
    private alertCtrl: AlertController,
    private location: Location,
    private route : ActivatedRoute
  ) {
    addIcons({addOutline, trashOutline});
  }

  ngOnInit() {
    // Récupération synchrone des données pré-chargées par les resolvers
    const resolvedData = this.route.snapshot.data;

    this.users = resolvedData['users'] || [];
    this.workers = resolvedData['workers'] || [];
    this.services = resolvedData['services'] || [];
    this.zones = resolvedData['zones'] || [];

    // Optionnel : tu peux aussi t'abonner si les données changent dynamiquement via .data.subscribe(...)
  }

  // Permet de fermer le dashboard ou de revenir à la page précédente
  goBack() {
    this.location.back();
  }

  onTabChange() {
    this.loadData();
  }

  loadData() {
    // 1. Chargement de la table globale des utilisateurs
    if (this.currentTab === 'users') {
      this.adminService.getUsers().subscribe({
        next: (data) => this.users = data,
        error: (err) => console.error('Erreur chargement utilisateurs', err)
      });
    }

    // 2. Chargement des workers pour l'onglet certifications
    if (this.currentTab === 'certifications') {
      this.adminService.getWorkers().subscribe({
        next: (data) => {
          this.workers = data;
          this.pendingCertificationsCount = this.getPendingCertifications().length;
        },
        error: (err) => console.error('Erreur chargement workers', err)
      });
    }

    // 3. Chargement des services
    if (this.currentTab === 'services') {
      // Si tu as un resolver ou une méthode dans l'adminService, tu peux l'appeler ici
      // Exemple : this.adminService.getServices()... ou via le WorkerService si tu passes par un resolver
      // Pour l'instant, on peut utiliser un appel via l'AdminService ou stocker un tableau s'il est résolu par la route.
    }

    // 4. Chargement des zones géographiques
    if (this.currentTab === 'zones') {
      // Idem, chargement des zones si nécessaire
    }

    // 5. Chargement des logs d'audit
    if (this.currentTab === 'logs') {
      this.adminService.getAuditLogs().subscribe({
        next: (logs) => this.auditLogs = logs,
        error: (err) => console.error('Erreur chargement logs d\'audit', err)
      });
    }
  }

  getPendingCertifications() {
    return this.workers
      .filter(w => w.certificationStatus === 'PENDING_APPROVAL')
      .sort((a, b) => {
        const dateA = new Date(a.certifiedAt || 0).getTime();
        const dateB = new Date(b.certifiedAt || 0).getTime();
        return dateA - dateB;
      });
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
      next: () => this.loadData(),
      error: (err) => console.error('Échec traitement certification', err)
    });
  }

  // ── GESTION DES SERVICES (Via AdminService) ────────────────────────
  deleteService(id: number) {
    if (confirm('Voulez-vous vraiment supprimer ce service ?')) {
      this.adminService.deleteService(id).subscribe({
        next: (updatedServices) => {
          this.services = updatedServices;
        },
        error: (err) => alert("Erreur lors de la suppression du service")
      });
    }
  }

  async openServiceModal() {
    const name = prompt("Nom du nouveau service :");
    if (!name) return;
    const rawDesc = prompt("Description (optionnelle) :");

    const payload: { id: number; name: string; description?: string } = { id: 0, name };

    // On ajoute la description seulement si elle existe et n'est pas vide
    if (rawDesc && rawDesc.trim() !== '') {
      payload.description = rawDesc.trim();
    }

    // Utilisation de la méthode dédiée dans l'adminService (ou création d'une méthode de sauvegarde globale)
    this.adminService.updateService(payload).subscribe({
      next: (res) => { this.services = res; },
      error: (err) => alert("Erreur : " + (err.error || "Conflit potentiel"))
    });
  }

  // ── GESTION DES ZONES (Via AdminService) ───────────────────────────
  deleteZone(id: number) {
    if (confirm('Voulez-vous vraiment supprimer cette zone ?')) {
      this.adminService.deleteRegion(id).subscribe({
        next: (updatedZones) => {
          this.zones = updatedZones;
        },
        error: (err) => {
          alert(err.error?.error || "Impossible de supprimer cette zone.");
        }
      });
    }
  }

  async openZoneModal() {
    const name = prompt("Nom de la nouvelle zone :");
    if (!name) return;
    const parentIdStr = prompt("ID du parent (laisser vide si c'est une racine) :");
    const parentId = parentIdStr ? parseInt(parentIdStr, 10) : 0;

    this.adminService.updateRegion({ id: 0, name, parentId }).subscribe({
      next: (res) => { this.zones = res; },
      error: (err) => alert("Erreur lors de la création de la zone")
    });
  }

  getZoneName(id:number):string{
    return this.zones.find((zone) => zone.id === id)?.name || "Zone inconnue";
  }
}
