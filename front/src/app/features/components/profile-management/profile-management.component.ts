import {Component, OnInit} from '@angular/core';
import {CommonModule} from "@angular/common";
import {IonicModule} from "@ionic/angular";
import {FormsModule} from "@angular/forms";
import {HeaderComponent} from "../header/header.component";
import {filter, map, Observable} from "rxjs";
import {BODY_TYPE_LABELS, EYE_COLOR_LABELS, HAIR_COLOR_LABELS, PhotoItem} from "../../models/items.model";
import {ActivatedRoute} from "@angular/router";
import {WorkerFullProfile, WorkerPrivateAccount, WorkerProfileUpdate} from "../../models/user.model";
import {WorkerAccountService} from "../../services/worker-account.service";
import {tap} from "rxjs/operators";
import {addIcons} from "ionicons";
import {addCircleOutline, camera, move, trashOutline, warningOutline, star, starOutline} from 'ionicons/icons';
import {AccountSettingsComponent} from "../account-settings/account-settings.component";
import {GeographicZone} from "../../models/filter.model";
import {ZoneSelectorComponent} from "../zone-selector/zone-selector.component";
import {environment} from "../../../../environments/environment";
import {StripePaymentComponent} from "../stripe-payment/stripe-payment.component";

@Component({
  selector: 'app-profile-management',
  imports: [CommonModule, FormsModule, IonicModule, HeaderComponent, AccountSettingsComponent, ZoneSelectorComponent, StripePaymentComponent],
  templateUrl: './profile-management.component.html',
  styleUrls: ['./profile-management.component.scss'],
  standalone: true
})
export class ProfileManagementComponent implements OnInit {

  activeTab     : 'profile' | 'photos' | 'settings' | 'subscription' = 'profile';

  currentUser$! : Observable<WorkerPrivateAccount>;
  allServices!  : string[];
  photos!: (PhotoItem & { isMain: boolean })[];
  allLocations! : GeographicZone[];
  childZoneId: number | undefined = undefined;
  parentZoneId: number | undefined = undefined;

  // Profile form
  lastServerState: WorkerProfileUpdate = {};
  profileForm: WorkerProfileUpdate = {};

  dragOverIndex: number | null = null;
  draggedIndex: number | null = null;
  isZoneActive: boolean = false;

  chosenAmount! : number;
  chosenType! : "DAYS" | "BOOST";

  constructor(private accountService: WorkerAccountService, private route : ActivatedRoute) {
    addIcons({
      addCircleOutline, trashOutline, move, camera, warningOutline, star, starOutline
    });
  }

  async ngOnInit(): Promise<void> {
    // 1. Chargement des données statiques
    this.route.data.subscribe((data) => {
      this.allServices = data['services'] || [];
      this.allLocations = data['locations'] || [];
    });

    // 2. Flux unique pour l'UI & Synchronisation automatique du formulaire
    this.currentUser$ = this.accountService.listenToMyAccount().pipe(
      filter((user): user is WorkerPrivateAccount => user !== null),
      tap(user => {

        // 🎯 On mappe en calculant le booléen 'isMain'
        this.photos = (user.photos || []).map(photo => {
          // Une photo est la principale si son URL brute correspond à celle du profil
          const isCurrentMain = photo.mainThumbUrl === user.mainThumbUrl;

          return {
            ...photo,
            isMain: isCurrentMain, // On injecte la propriété manquante ici !
            previewThumbUrl: photo.previewThumbUrl?.startsWith('http')
              ? photo.previewThumbUrl
              : `${environment.apiBase}${photo.previewThumbUrl}`,
            mainThumbUrl: photo.mainThumbUrl?.startsWith('http')
              ? photo.mainThumbUrl
              : `${environment.apiBase}${photo.mainThumbUrl}`
          };
        });

        this.childZoneId = user.geographicZone?.id;

        if (this.childZoneId) {
          // On cherche le parent dans "allLocations" qui possède cette sous-zone
          const parentZone = this.allLocations.find(parent =>
            parent.subZones?.some(sub => sub.id === this.childZoneId)
          );

          // Si on a trouvé un parent, on stocke son ID, sinon ça veut dire que l'utilisateur a sélectionné une zone qui est déjà un parent
          this.parentZoneId = parentZone ? parentZone.id : this.childZoneId;
        } else {
          this.parentZoneId = undefined;
        }

        let cleanBirthdate = '';
        if (user.birthdate) {
          const d = new Date(user.birthdate);
          if (!isNaN(d.getTime())) {
            cleanBirthdate = d.toISOString().split('T')[0];
          }
        }

        this.lastServerState = {
          username : user.username,
          bodyType: user.bodyType,
          geographicZoneId: user.geographicZone?.id,
          description: user.description,
          phone: user.phone,
          birthdate: cleanBirthdate
        };

        this.profileForm = { ...this.lastServerState };
      })
    );
  }

  isFieldMissing(field: string, me: any): boolean {
    if (!me) return true;
    switch (field) {
      case 'username':return !me.username || me.username.trim() === '';
      case 'email':
        const emailStr = typeof me.email === 'object' ? me.email?.value : me.email;
        return !emailStr || emailStr.trim() === '';
      case 'description':return !me.description || me.description.trim() === '';
      case 'geographicZoneId':return !me.geographicZone || !me.geographicZone.id || me.geographicZone.id == -1;
      case 'phone':return !me.phone || me.phone.trim() === '';
      case 'services':return !me.services || me.services.length === 0;
      case 'photos':return !this.photos || this.photos.length === 0;
      case 'birthday':return !me.birthdate;
      default:return false;
    }
  }

  getMissingFieldsList(me: any): string[] {
    const missing: string[] = [];
    if (!me) return missing;

    if (this.isFieldMissing('username', me)) missing.push("Nom d'utilisateur");
    if (this.isFieldMissing('email', me)) missing.push('Email');
    if (this.isFieldMissing('description', me)) missing.push('Description');
    if (this.isFieldMissing('geographicZoneId', me)) missing.push('Localisation');
    if (this.isFieldMissing('phone', me)) missing.push('Téléphone');
    if (this.isFieldMissing('birthday', me)) missing.push('Date de naissance');
    if (this.isFieldMissing('services', me)) missing.push('Au moins 1 service');
    if (this.isFieldMissing('photos', me)) missing.push('Au moins 1 photo');

    return missing;
  }

  // ── Value modification ─────────────────────────────────────────────────────────

  toggleService(me: WorkerFullProfile, service: string) {
    // 1. Calculer la nouvelle liste localement
    let updatedServices = [...me.services];
    if (updatedServices.includes(service)) {
      updatedServices = updatedServices.filter(s => s !== service);
    } else {
      updatedServices.push(service);
    }
    console.log("Services à envoyer au serveur:", updatedServices);

    this.accountService.updateServices(updatedServices);
  }

  async toggleAvailable(currentAvailability: boolean) {
    console.log("toggling availability to", currentAvailability);
    try {
      await this.accountService.setAvailability(currentAvailability);
    } catch (error) {
      console.error("Erreur lors du changement de disponibilité", error);
    }
  }

  async updateProfileField(field: keyof WorkerProfileUpdate, value: any) {
    if (this.lastServerState[field] === value) {
      return;
    }

    console.log(`Envoi au serveur pour [${field}] :`, value);
    try {
      await this.accountService.updateProfile({ [field]: value });
      this.lastServerState[field] = value;
    } catch (error) {
      console.error(`Échec de la mise à jour pour ${field}`, error);
      // En cas d'erreur réseau, on remet l'ancienne valeur du serveur dans l'input
      (this.profileForm as any)[field] = this.lastServerState[field];
    }
  }

  // ── Photos ────────────────────────────────────────────────────────────────

  async onFileSelected(event: any) {
    const files: FileList = event.target.files;
    if (!files || files.length === 0) return;

    await this.processAndUploadFiles(Array.from(files));
    event.target.value = ''; // Reset l'input pour pouvoir ré-uploader les mêmes fichiers si besoin
  }

// 🎯 Gestion du survol de la zone de drop
  onZoneDragOver(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isZoneActive = true;
  }

  onZoneDragLeave() {
    this.isZoneActive = false;
  }

// 🎯 Déclenché quand on lâche les fichiers dans la zone de drop
  async onZoneDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isZoneActive = false;

    if (event.dataTransfer && event.dataTransfer.files.length > 0) {
      await this.processAndUploadFiles(Array.from(event.dataTransfer.files));
    }
  }

// 🎯 Cœur de la logique : Filtrage et upload en série ou parallèle
  private async processAndUploadFiles(fileList: File[]) {
    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
    const validFiles: File[] = [];

    for (const file of fileList) {
      // Vérification du type MIME ou de l'extension (au cas où le type MIME est vide avec .heic)
      const isExtensionInvalid = file.name.toLowerCase().endsWith('.heic') || file.name.toLowerCase().endsWith('.pdf');

      if (!allowedTypes.includes(file.type) || isExtensionInvalid) {
        // Tu peux remplacer ce console.warn par une alerte Toast ou Modale Ionic pour l'utilisateur
        console.warn(`Fichier refusé (format non supporté) : ${file.name}`);
        continue;
      }

      validFiles.push(file);
    }

    if (validFiles.length === 0) {
      // Afficher une alerte à l'utilisateur ici si tu veux
      return;
    }

    // Upload des fichiers valides un par un (pour éviter de surcharger le endpoint)
    for (const file of validFiles) {
      try {
        await this.accountService.uploadPhoto(file);
      } catch (error) {
        console.error(`Échec de l'upload pour ${file.name}`, error);
      }
    }
  }

  setMain(photo: PhotoItem) {
    this.accountService.setMainPhoto(photo.id);
  }

  deletePhoto(photo: PhotoItem) {
    this.accountService.deletePhoto(photo.id);
  }

  // DRAG AND DROP REORDER

  onDragStart(index: number) {
    this.draggedIndex = index;
  }

  onDragOver(event: DragEvent, index: number) {
    event.preventDefault(); // Indispensable pour autoriser le "drop"
    this.dragOverIndex = index;
  }

  async onDrop(targetIndex: number) {
    if (this.draggedIndex === null || this.draggedIndex === targetIndex) return;

    // 1. On réorganise le tableau localement pour un rendu visuel instantané
    const movedPhoto = this.photos[this.draggedIndex];
    this.photos.splice(this.draggedIndex, 1);       // Supprime de l'ancienne position
    this.photos.splice(targetIndex, 0, movedPhoto); // Insère à la nouvelle position

    // Reset des index de drag
    this.draggedIndex  = null;
    this.dragOverIndex = null;

    // 2. On extrait les IDs ordonnés et on synchronise avec le serveur
    const orderedIds = this.photos.map(p => p.id);
    try {
      await this.accountService.reorderPhotos(orderedIds);
    } catch (error) {
      console.error("Erreur lors de la sauvegarde de l'ordre des photos", error);
    }
  }

  onDragEnd() {
    this.draggedIndex = null;
    this.dragOverIndex = null;
  }

  // ── Settings ──────────────────────────────────────────────────────────────

  async handleSettingsSave(event: any) {
    const { payload, setLoading, setSuccess, setError } = event;

    try {
      await this.accountService.updateProfileData(payload);
      setSuccess('Vos paramètres de compte ont été mis à jour avec succès.');
    } catch (err: any) {
      setError(err?.error?.message || 'Une erreur est survenue lors de la mise à jour.');
    } finally {
      setLoading(false);
    }
  }

  handleSubscriptionClick() {
    console.log("Redirection vers la passerelle de paiement / Stripe / etc.");
    // Votre logique de modal ou de redirection vers le renouvellement
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  get subscriptionHoursLeft$(): Observable<number> {
    return this.currentUser$.pipe(
      map(profile => {
        // Si le profil n'existe pas ou s'il n'a pas de date d'expiration
        if (!profile || !profile.expirationDate) {
          return 0;
        }

        const expiryTime = new Date(profile.expirationDate).getTime();
        const currentTime = new Date().getTime();
        const diffInMs = expiryTime - currentTime;

        // Convertir les millisecondes en heures et arrondir à l'inférieur
        const hoursLeft = Math.floor(diffInMs / (1000 * 60 * 60));

        // Retourner 0 si l'abonnement est déjà expiré (valeur négative)
        return hoursLeft > 0 ? hoursLeft : 0;
      })
    );
  }

  get subscriptionColor$(): Observable<string> {
    return this.subscriptionHoursLeft$.pipe(
      map(hours => {
        if (hours <= 24) return 'danger';
        if (hours <= 168) return 'warning'; // 168 heures = 7 jours
        return 'success';
      })
    );
  }

  get subscriptionLabel$(): Observable<string> {
    return this.subscriptionHoursLeft$.pipe(
      map(hours => {
        if (hours <= 0) {
          return 'Abonnement expiré';
        }

        // S'il reste plus de 48 heures, on affiche en Jours
        if (hours > 48) {
          const days = Math.floor(hours / 24);
          return `${days} jour${days > 1 ? 's' : ''} restant${days > 1 ? 's' : ''}`;
        }

        // S'il reste moins de 48 heures, on affiche précisément en Heures
        return `${hours} heure${hours > 1 ? 's' : ''} restante${hours > 1 ? 's' : ''}`;
      })
    );
  }

  protected readonly BODY_TYPE_LABELS = BODY_TYPE_LABELS;
  protected readonly HAIR_COLOR_LABELS = HAIR_COLOR_LABELS;
  protected readonly EYE_COLOR_LABELS = EYE_COLOR_LABELS;
  protected readonly environment = environment;
  protected readonly isNaN = isNaN;
}
