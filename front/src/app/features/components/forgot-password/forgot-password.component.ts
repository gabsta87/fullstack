import {Component, OnInit} from '@angular/core';
import {AuthService} from "../../services/auth.service";
import {IonButton, IonContent, IonInput, IonItem, LoadingController, ModalController} from "@ionic/angular/standalone";
import {AlertController} from "@ionic/angular";
import {FormsModule} from "@angular/forms";

@Component({
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.scss'],
  standalone: true,
  imports: [IonContent, FormsModule, IonItem, IonInput, IonButton]
})
export class ForgotPasswordComponent implements OnInit {
  email = '';

  constructor(
    private authService: AuthService,
    private loadingCtrl: LoadingController,
    private alertCtrl: AlertController,
    private modalCtrl: ModalController
  ) {}

  ngOnInit() {
    // Forcer le focus dès l'affichage du composant dans la modale
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
        // Sécurité anti-énumération d'e-mails
        await this.showConfirmationAlert("Si cette adresse est enregistrée chez nous, un e-mail de réinitialisation vient de vous être envoyé.");
      }
    });
  }

  private async showConfirmationAlert(message: string) {
    const alert = await this.alertCtrl.create({
      header: 'E-mail envoyé',
      message: message,
      backdropDismiss: false, // L'utilisateur doit obligatoirement cliquer sur le bouton.
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

  // Permet aussi de fermer via un éventuel bouton "Annuler" dans ton HTML
  close() {
    this.modalCtrl.dismiss();
  }
}
