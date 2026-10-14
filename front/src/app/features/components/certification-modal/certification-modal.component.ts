import {Component, Input, OnInit} from '@angular/core';
import {IonHeader, ModalController, IonToolbar, IonTitle, IonButtons, IonButton, IonContent, IonIcon} from "@ionic/angular/standalone";
import {addIcons} from "ionicons";
import { alertCircleOutline, cloudUploadOutline, sendOutline } from "ionicons/icons";
import {AsyncPipe, NgIf} from "@angular/common";
import {LanguageManagerService} from "../../services/language-manager.service";

@Component({
  selector: 'app-certification-modal',
  templateUrl: './certification-modal.component.html',
  styleUrls: ['./certification-modal.component.scss'],
  standalone : true,
  imports: [IonHeader, IonToolbar, IonTitle, IonButtons, IonButton, IonContent, IonIcon, NgIf, AsyncPipe],
})
export class CertificationModalComponent implements OnInit {
  @Input() verificationCode!: string;
  @Input() adminMessage?: string; // Message de l'admin si NEEDS_REVISION

  selectedFile: File | null = null;

  constructor(private modalController: ModalController, protected languageService : LanguageManagerService) {
    addIcons({
      alertCircleOutline, cloudUploadOutline, sendOutline
    });
  }

  ngOnInit() {}

  dismiss() {
    this.modalController.dismiss();
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
    }
  }

  submitCertificationPhoto() {
    if (!this.selectedFile) return;

    // On renvoie le fichier sélectionné au composant parent via le modalController
    this.modalController.dismiss({
      file: this.selectedFile
    }, 'submitted');
  }
}
