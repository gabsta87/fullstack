import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {AlertController, IonicModule} from '@ionic/angular';
import {WorkerProfileForAdmin} from "../../models/user.model";
import {AdminService} from "../../services/admin-service";

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {
  currentTab: 'users' | 'certifications' | 'logs' = 'users';
  workers: WorkerProfileForAdmin[] = [];
  auditLogs: any[] = [];
  pendingCertificationsCount = 0;

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
    // Récupération globale des profils via le service
    this.adminService.getAllProfiles().subscribe({
      next: (data) => {
        this.workers = data;
        this.pendingCertificationsCount = this.getPendingCertifications().length;
      },
      error: (err) => console.error('Erreur chargement profils admin', err)
    });

    // Si on navigue sur l'onglet logs, on charge l'historique d'audit
    if (this.currentTab === 'logs') {
      this.adminService.getAuditLogs().subscribe({
        next: (logs) => this.auditLogs = logs,
        error: (err) => console.error('Erreur chargement logs audit', err)
      });
    }
  }

  getPendingCertifications() {
    return this.workers.filter(w => w.certificationStatus === 'PENDING_APPROVAL');
  }

  toggleStatus(worker: any) {
    this.adminService.toggleStatus(worker.id).subscribe({
      next: (res) => {
        if (res.success) {
          // Si isActive est vrai (en ligne), alors disabled doit être faux
          worker.disabled = !res.isActive;
        }
      },
      error: (err) => console.error('Échec du basculement de statut', err)
    });
  }

  async promptUpdateDays(worker: any) {
    const alert = await this.alertCtrl.create({
      header: `Ajuster l'abonnement de ${worker.username || worker.name}`,
      inputs: [
        { name: 'days', type: 'number', value: worker.remainingDaysCredit, placeholder: 'Nombre de jours' },
        { name: 'reason', type: 'text', placeholder: 'Motif de la modification (Requis)' }
      ],
      buttons: [
        { text: 'Annuler', role: 'cancel' },
        {
          text: 'Enregistrer',
          handler: (data) => {
            if (!data.reason || data.reason.trim() === '') return false; // Bloquer si aucun motif n'est saisi

            this.adminService.updateDaysCredit(worker.id, parseInt(data.days, 10), data.reason).subscribe({
              next: (res) => {
                if (res.success) {
                  worker.remainingDaysCredit = res.newDaysValue;
                }
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
          {
            text: 'Valider',
            handler: (data) => {
              reason = data.reason;
            }
          }
        ]
      });
      await alertReason.present();
      const result = await alertReason.onDidDismiss();
      if (result.role === 'cancel') return; // On annule l'opération complète si le motif est annulé
    }

    this.adminService.verifyCertification(workerId, approved, reason).subscribe({
      next: () => this.loadData(),
      error: (err) => console.error('Échec traitement certification', err)
    });
  }
}
