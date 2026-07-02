import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule, AlertController } from '@ionic/angular';
import {environment} from "../../../../environments/environment";

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {
  currentTab: 'users' | 'certifications' | 'logs' = 'users';
  workers: any[] = [];
  auditLogs: any[] = [];
  pendingCertificationsCount = 0;

  // Mock temporaire pour simuler l'admin connecté (à remplacer par ton service d'authentification)
  adminId = "ADMIN-UUID-1234";
  adminUsername = "ChefModo_Venus";

  constructor(private http: HttpClient, private alertCtrl: AlertController) {}

  ngOnInit() {
    this.loadData();
  }

  onTabChange() {
    this.loadData();
  }

  private getAdminHeaders(): HttpHeaders {
    return new HttpHeaders({
      'X-Admin-Id': this.adminId,
      'X-Admin-Username': this.adminUsername
    });
  }

  loadData() {
    // Récupération globale des profils
    this.http.get<any[]>(`${environment.apiBase}/api/admin/profiles`, { headers: this.getAdminHeaders() })
      .subscribe(data => {
        this.workers = data;
        this.pendingCertificationsCount = this.getPendingCertifications().length;
      });

    // Si on est sur l'onglet logs, on charge l'historique d'audit
    if (this.currentTab === 'logs') {
      this.http.get<any[]>(`${environment.apiBase}/api/admin/logs`, { headers: this.getAdminHeaders() })
        .subscribe(logs => this.auditLogs = logs);
    }
  }

  getPendingCertifications() {
    return this.workers.filter(w => w.certificationStatus === 'PENDING_APPROVAL');
  }

  toggleStatus(worker: any) {
    this.http.post<any>(`${environment.apiBase}/api/admin/profiles/${worker.id}/toggle-status`, {}, { headers: this.getAdminHeaders() })
      .subscribe(res => {
        if (res.success) {
          worker.active = res.isActive;
        }
      });
  }

  async promptUpdateDays(worker: any) {
    const alert = await this.alertCtrl.create({
      header: `Ajuster l'abonnement de ${worker.name}`,
      inputs: [
        { name: 'days', type: 'number', value: worker.remainingDaysCredit, placeholder: 'Nombre de jours' },
        { name: 'reason', type: 'text', placeholder: 'Motif de la modification (Requis)' }
      ],
      buttons: [
        { text: 'Annuler', role: 'cancel' },
        {
          text: 'Enregistrer',
          handler: (data) => {
            if (!data.reason) return false; // Bloquer si aucun motif n'est saisi

            const payload = { workerId: worker.id, newDaysValue: parseInt(data.days, 10), reason: data.reason };
            this.http.post<any>(`${environment.apiBase}/api/admin/profiles/update-days`, payload, { headers: this.getAdminHeaders() })
              .subscribe(res => {
                if (res.success) {
                  worker.remainingDaysCredit = res.newDaysValue;
                }
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
        inputs: [{ name: 'reason', type: 'text', placeholder: 'Ex: Panneau illisible' }],
        buttons: [{ text: 'Valider', handler: (data) => { reason = data.reason; } }]
      });
      await alertReason.present();
      await alertReason.onDidDismiss();
    }

    const payload = { workerId, approved, rejectionReason: reason };
    this.http.post<any>(`${environment.apiBase}/api/admin/profiles/verify-certification`, payload, { headers: this.getAdminHeaders() })
      .subscribe(() => this.loadData());
  }
}
