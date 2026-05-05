import { Component } from '@angular/core';
import { Router, RouterLink } from "@angular/router";
import { AuthService } from "../../services/auth.service";
import { AccountService } from "../../services/account.service";
import { CommonModule } from "@angular/common";
import { IonicModule, ModalController } from "@ionic/angular"; // Ajout ModalController
import { AuthModalComponent } from "../auth-modal/auth-modal.component"; // Chemin à vérifier

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, IonicModule, RouterLink],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss'],
})
export class HeaderComponent {

  constructor(
    public authService: AuthService,
    private accountService: AccountService,
    private router: Router,
    private modalCtrl: ModalController // Injecté ici
  ) {}

  // Fonction centrale pour ouvrir la modale
  async openAuth(mode: 'login' | 'register') {
    const modal = await this.modalCtrl.create({
      component: AuthModalComponent,
      componentProps: { mode }
    });

    await modal.present();

    // On attend la fermeture pour voir si on doit rediriger
    const { data } = await modal.onWillDismiss();
    if (data === true) {
      // Si la modale a renvoyé 'true' (succès), on gère la redirection ici
      this.openAccount();
    }
  }

  onLogout() {
    this.authService.logout().subscribe(() => {
      this.router.navigate(['/']);
    });
  }

  openAccount() {
    this.accountService.getMe().subscribe({
      next: (user) => {
        const target = user.role === 'WORKER' ? '/profile-management' : '/account';
        this.router.navigate([target]);
      },
      error: () => this.router.navigate(['/account'])
    });
  }
}
