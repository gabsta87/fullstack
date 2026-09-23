import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AlertController, IonicModule } from '@ionic/angular';
import { TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { TagModule } from 'primeng/tag';
import { AdminService } from '../../services/admin-service';

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
  currentTab: 'users' | 'certifications' | 'logs' = 'users';

  users: any[] = [];
  workers: any[] = []; // Liste dédiée pour les certifications
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
    private alertCtrl: AlertController
  ) {}

  ngOnInit() {
    this.loadData();
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

    // 3. Chargement des logs d'audit
    if (this.currentTab === 'logs') {
      this.adminService.getAuditLogs().subscribe({
        next: (logs) => this.auditLogs = logs,
        error: (err) => console.error('Erreur chargement logs d\'audit', err)
      });
    }
  }

  /**
   * Filtre les workers en attente de certification et les trie
   * par date de demande (du plus ancien au plus récent, ou inversement via certifiedAt/createdAt)
   */
  getPendingCertifications() {
    return this.workers
      .filter(w => w.certificationStatus === 'PENDING_APPROVAL')
      .sort((a, b) => {
        // Tri par date (exemple basé sur certifiedAt ou une date de soumission)
        const dateA = new Date(a.certifiedAt || 0).getTime();
        const dateB = new Date(b.certifiedAt || 0).getTime();
        return dateA - dateB; // Du plus ancien au plus récent
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
}
