import {Component, Input, OnInit, ChangeDetectionStrategy} from '@angular/core';
import {ModalController} from "@ionic/angular/standalone";
import {addIcons} from "ionicons";
import {alertCircleOutline, cloudUploadOutline, sendOutline, refreshOutline} from "ionicons/icons";
import {CommonModule} from "@angular/common";
import {LanguageManagerService} from "../../services/language-manager.service";
import {IonicModule} from "@ionic/angular";

@Component({
    selector: 'app-certification-modal',
    templateUrl: './certification-modal.component.html',
    styleUrls: ['./certification-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [IonicModule, CommonModule]
})
export class CertificationModalComponent implements OnInit {
  @Input() verificationCode!: string;
  @Input() adminMessage?: string;
  @Input() existingPhotoUrl?: string; // Optional: the user may already have submitted a photo

  selectedFile: File | null = null;
  previewUrl: string | null = null;

  constructor(
    private modalController: ModalController,
    protected languageService: LanguageManagerService
  ) {
    addIcons({
      alertCircleOutline, cloudUploadOutline, sendOutline, refreshOutline
    });
  }

  ngOnInit() {
    // Show the existing photo in the preview
    if (this.existingPhotoUrl) {
      this.previewUrl = this.existingPhotoUrl;
    }
  }

  dismiss() {
    this.modalController.dismiss();
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      // Temporary URL to preview the new photo right away
      this.previewUrl = URL.createObjectURL(file);
    }
  }

  submitCertificationPhoto() {
    if (!this.selectedFile && !this.existingPhotoUrl) return;

    this.modalController.dismiss({
      file: this.selectedFile
    }, 'submitted');
  }
}
