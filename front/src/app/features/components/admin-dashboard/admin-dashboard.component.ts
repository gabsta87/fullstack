import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AlertController, IonicModule } from '@ionic/angular';
import { TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { TagModule } from 'primeng/tag';
import { AdminService } from '../../services/admin-service';
import { addIcons } from "ionicons";
import { addOutline, pencilOutline, trashOutline } from "ionicons/icons";
import { ActivatedRoute } from "@angular/router";
import { firstValueFrom, map, Observable, Subject } from "rxjs";
import { switchMap, tap } from "rxjs/operators";
import { CertificationRequest, Service } from "../../models/common.model";
import { GeographicZoneWithParent } from "../../models/filter.model";
import { WorkerPrivateAccount, WorkerProfileForAdmin } from "../../models/user.model";
import { AuthService } from "../../services/auth.service";

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule, TableModule, InputTextModule, DropdownModule, TagModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit, OnDestroy {
  currentTab: 'users' | 'certifications' | 'services' | 'zones' | 'logs' = 'users';

  users$!: Observable<WorkerProfileForAdmin[]>;
  pendingCertificationRequests$!: Observable<CertificationRequest[]>;
  services$!: Observable<Service[]>;
  zones$!: Observable<GeographicZoneWithParent[]>;
  auditLogs$!: Observable<any[]>;
  flatZones$!: Observable<{ id: number; name: string; level: number; parentId: number | null }[]>;

  selectedWorkerDetails: WorkerPrivateAccount | null = null;
  isCertifModalOpen: boolean = false;
  currentCertifRequestId: number | null = null;
  adminComment: string = '';

  private submitCertification$ = new Subject<{ requestId: number; approved: boolean; comment: string }>();

  selectedRole: string | null = null;
  roleOptions = [
    { label: 'Tous les rôles', value: null },
    { label: 'Administrateur', value: 'ADMIN' },
    { label: 'Client', value: 'CLIENT' },
    { label: 'Annonceur', value: 'WORKER' }
  ];

  activePreviewUrl: string | null = null;
  previewImagesList: string[] = [];
  previewIndex: number = 0;
  certificationPreviewList: string[] = [];

  constructor(
    private adminService: AdminService,
    private alertCtrl: AlertController,
    private location: Location,
    private route: ActivatedRoute,
    private authService: AuthService,
  ) {
    addIcons({ addOutline, trashOutline, pencilOutline });
  }

  // 💡 Déverrouillage automatique si l'admin ferme ou rafraîchit la page en plein milieu d'une modération
  @HostListener('window:beforeunload', ['$event'])
  unloadHandler($event: BeforeUnloadEvent) {
    if (this.currentCertifRequestId) {
      // Utilisation synchrone / sendBeacon ou appel direct de secours si possible,
      // ou on s'appuie sur le mécanisme de timeout côté backend s'il existe.
      this.adminService.unlockCertificationReview(this.currentCertifRequestId).subscribe();
    }
  }

  ngOnInit() {
    this.users$ = this.route.data.pipe(map(data => data['users'] || []));
    this.auditLogs$ = this.route.data.pipe(map(data => data['logs'] || []));

    this.pendingCertificationRequests$ = this.adminService.pendingCertificationRequests$.pipe(
      map(requests => (requests || []).sort((a: any, b: any) =>
        new Date(a.createdAt || 0).getTime() - new Date(b.createdAt || 0).getTime()
      ))
    );

    this.services$ = this.adminService.services$;
    this.zones$ = this.adminService.zones$;

    this.flatZones$ = this.zones$.pipe(
      map(zones => {
        if (!zones) return [];
        const parents = zones.filter(z => !z.parentId);
        const result: { id: number; name: string; level: number; parentId: number | null }[] = [];

        parents.forEach(parent => {
          result.push({ id: parent.id, name: parent.name, level: 0, parentId: parent.parentId ?? null });
          const children = zones.filter(z => z.parentId === parent.id);
          children.forEach(child => {
            result.push({ id: child.id, name: child.name, level: 1, parentId: parent.id });
          });
        });

        return result;
      })
    );

    // 💡 Pipeline réactif de soumission : gère l'appel HTTP et la fermeture de modale automatiquement sans .subscribe() verbeux
    this.submitCertification$.pipe(
      switchMap(({ requestId, approved, comment }) =>
        this.adminService.verifyCertification(requestId, approved, comment).pipe(
          tap(() => this.closeCertificationModal())
        )
      )
    ).subscribe({
      error: (err) => {
        console.error('Échec traitement certification', err);
        alert("Une erreur est survenue lors du traitement de la requête.");
      }
    });

    // Chargement initial
    this.adminService.refreshCertifications();
    this.adminService.refreshServices();
    this.adminService.refreshZones();
  }

  ngOnDestroy() {
    // Nettoyage de sécurité si le composant est détruit
    if (this.currentCertifRequestId) {
      this.adminService.unlockCertificationReview(this.currentCertifRequestId).subscribe();
    }
  }

  goBack() {
    this.location.back();
  }

  setLocked(user: any, lockState: boolean) {
    this.adminService.updateWorkerStatus(user.id, { locked: lockState }).subscribe({
      next: (updatedWorker) => { user.locked = updatedWorker.locked; },
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
              next: (res: any) => { user.remainingDaysCredit = res.newDaysValue; },
              error: (err) => console.error('Échec mise à jour crédit jours', err)
            });
            return true;
          }
        }
      ]
    });
    await alert.present();
  }

  // ── GESTION DES SERVICES & ZONES ──────────────────────────────────────────

  async openServiceModal() {
    const name = prompt("Nom du nouveau service :");
    if (!name || name.trim() === '') return;
    const description = prompt("Description (optionnelle) :") || undefined;

    try {
      await firstValueFrom(this.adminService.updateService({
        name: name.trim(),
        description: description ? description.trim() : undefined
      }));
    } catch (err: any) {
      alert("Erreur : " + (err.error || "Impossible de créer le service"));
    }
  }

  async editServiceModal(service: any) {
    const name = prompt("Modifier le nom du service :", service.name);
    if (name === null) return;
    if (name.trim() === '') {
      alert("Le nom du service ne peut pas être vide.");
      return;
    }

    const description = prompt("Modifier la description :", service.description || '');
    if (description === null) return;

    try {
      await firstValueFrom(this.adminService.updateService({
        id: service.id,
        name: name.trim(),
        description: description.trim()
      }));
    } catch (err:any) {
      alert("Erreur : " + (err.error || "Impossible de modifier le service"));
    }
  }

  async deleteService(id: number) {
    if (confirm('Voulez-vous vraiment supprimer ce service ?')) {
      try {
        await firstValueFrom(this.adminService.deleteService(id));
      } catch (err: any) {
        alert("Erreur lors de la suppression du service : " + (err.error?.error || "Erreur inconnue"));
      }
    }
  }

  async openZoneModal(parentId: number | null = null) {
    const title = parentId ? "Nom de la nouvelle sous-zone :" : "Nom de la nouvelle zone parente :";
    const name = prompt(title);
    if (!name || name.trim() === '') return;

    try {
      await firstValueFrom(this.adminService.updateRegion({
        name: name.trim(),
        ...(parentId !== null ? { parentId } : {})
      }));
    } catch (err: any) {
      alert(err.error?.error || "Erreur lors de la création de la zone");
    }
  }

  async editZoneName(zone: any) {
    const newName = prompt("Modifier le nom de la zone :", zone.name);
    if (!newName || newName.trim() === '' || newName === zone.name) return;

    try {
      await firstValueFrom(this.adminService.updateRegion({
        id: zone.id,
        name: newName.trim(),
        parentId: zone.parentId
      }));
    } catch (err: any) {
      alert(err.error?.error || "Erreur lors de la modification de la zone");
    }
  }

  async deleteZone(id: number) {
    if (confirm('Voulez-vous vraiment supprimer cette zone ?')) {
      try {
        await firstValueFrom(this.adminService.deleteRegion(id));
      } catch (err: any) {
        alert(err.error?.error || "Impossible de supprimer cette zone.");
      }
    }
  }

  // ── CERTIFICATIONS ────────────────────────────────────────────────────────

  async openCertificationModal(request: CertificationRequest) {
    const tokenData = this.authService.getDecodedToken();
    const currentAdminId = tokenData?.id || tokenData?.userId;
    const isLockedByOther = request.underReview && request.lockedByAdminId && request.lockedByAdminId !== currentAdminId;

    if (isLockedByOther) {
      alert("Cette requête est déjà en cours de traitement par un autre administrateur.");
      return;
    }

    this.currentCertifRequestId = request.id;
    this.adminComment = '';

    try {
      await firstValueFrom(this.adminService.lockCertificationReview(request.id, true));

      const workerDetails = await firstValueFrom(
        this.adminService.getWorkerProfileForAdmin(request.workerId)
      );

      const formattedCertifUrl = request.certificationPhotoUrl ?? "";

      this.selectedWorkerDetails = {
        ...workerDetails,
        certificationRequestId: request.id,
        verificationCode: request.verificationCode,
        certificationPhotoUrl: formattedCertifUrl
      } as any;

      const galleryUrls = (workerDetails.photos || []).map(p => p.mainThumbUrl);
      this.certificationPreviewList = [formattedCertifUrl, ...galleryUrls];

      this.isCertifModalOpen = true;
    } catch (err) {
      console.error("Erreur lors du verrouillage ou du chargement de la requête", err);
      alert("Impossible d'ouvrir cette requête.");
      this.currentCertifRequestId = null;
    }
  }

  async closeCertificationModal() {
    if (this.currentCertifRequestId) {
      try {
        await firstValueFrom(this.adminService.unlockCertificationReview(this.currentCertifRequestId));
      } catch (err) {
        console.error("Erreur lors du déverrouillage de la requête", err);
      }
    }

    this.isCertifModalOpen = false;
    this.selectedWorkerDetails = null;
    this.currentCertifRequestId = null;
    this.adminComment = '';
  }

  // 💡 Soumission ultra-épurée grâce au flux réactif mis en place dans ngOnInit
  submitCertificationReview(approved: boolean) {
    const requestId = this.currentCertifRequestId || (this.selectedWorkerDetails as any)?.certificationRequestId;

    if (!requestId) {
      alert("Erreur : Aucun identifiant de requête trouvé.");
      return;
    }

    const comment = this.adminComment.trim();

    if (!approved && !comment) {
      alert("Veuillez saisir un commentaire ou un motif pour le refus / la demande de complément.");
      return;
    }

    // On pousse simplement les données dans le flux, le switchMap s'occupe du reste
    this.submitCertification$.next({ requestId, approved, comment });
  }

  // ── GESTION DE LA LIGHTBOX D'IMAGES ──────────────────────────────────────

  openImagePreview(url: string, allImages: string[] = []) {
    this.activePreviewUrl = url;
    this.previewImagesList = allImages;
    this.previewIndex = allImages.indexOf(url);
  }

  closeImagePreview() {
    this.activePreviewUrl = null;
    this.previewImagesList = [];
    this.previewIndex = 0;
  }

  nextPreviewImage() {
    if (this.previewImagesList.length === 0) return;
    this.previewIndex = (this.previewIndex + 1) % this.previewImagesList.length;
    this.activePreviewUrl = this.previewImagesList[this.previewIndex];
  }

  prevPreviewImage() {
    if (this.previewImagesList.length === 0) return;
    this.previewIndex = (this.previewIndex - 1 + this.previewImagesList.length) % this.previewImagesList.length;
    this.activePreviewUrl = this.previewImagesList[this.previewIndex];
  }
}
