import {Component, OnInit, ChangeDetectionStrategy} from '@angular/core';
import {AuthService} from "../../services/auth.service";
import {IonButton, IonContent, IonInput, IonItem, LoadingController, ModalController} from "@ionic/angular/standalone";
import {AlertController} from "@ionic/angular";
import {FormsModule} from "@angular/forms";
import {LanguageManagerService} from "../../services/language-manager.service";
import { AsyncPipe } from "@angular/common";

@Component({
    selector: 'app-forgot-password',
    templateUrl: './forgot-password.component.html',
    styleUrls: ['./forgot-password.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [IonContent, FormsModule, IonItem, IonInput, IonButton, AsyncPipe]
})
export class ForgotPasswordComponent implements OnInit {
  email = '';

  constructor(
    private authService: AuthService,
    private loadingCtrl: LoadingController,
    private alertCtrl: AlertController,
    private modalCtrl: ModalController,
    protected languageService : LanguageManagerService,
  ) {}

  ngOnInit() {
    // Focus the field as soon as the modal shows
    setTimeout(() => {
      const inputEl = document.querySelector('app-forgot-password ion-input') as any;
      if (inputEl) {
        inputEl.setFocus();
      }
    }, 300);
  }

  async sendRequest() {
    if (!this.email || !this.email.trim()) return;

    const loading = await this.loadingCtrl.create({
      message: 'Envoi du lien...',
      spinner: 'crescent'
    });
    await loading.present();

    this.authService.requestPasswordReset(this.email).subscribe({
      next: async (response) => {
        await loading.dismiss();
        await this.showConfirmationAlert(response);
      },
      error: async () => {
        await loading.dismiss();
        // Same answer whatever the email (no account enumeration)
        await this.showConfirmationAlert("Si cette adresse est enregistrée chez nous, un e-mail de réinitialisation vient de vous être envoyé.");
      }
    });
  }

  private async showConfirmationAlert(message: string) {
    const alert = await this.alertCtrl.create({
      header: 'E-mail envoyé',
      message: message,
      backdropDismiss: false, // The user must click the button.
      buttons: [
        {
          text: 'Fermer',
          handler: () => {
            this.modalCtrl.dismiss();
          }
        }
      ]
    });
    await alert.present();
  }

  // Also closes from a "Cancel" button
  close() {
    this.modalCtrl.dismiss();
  }
}
