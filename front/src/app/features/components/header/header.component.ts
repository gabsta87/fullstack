import { Component } from '@angular/core';
import { Router, RouterLink } from "@angular/router";
import { AuthService } from "../../services/auth.service";
import { ClientAccountService } from "../../services/client-account.service";
import { CommonModule } from "@angular/common";
import { IonicModule, ModalController } from "@ionic/angular"; // Ajout ModalController
import { AuthModalComponent } from "../auth-modal/auth-modal.component";
import {firstValueFrom} from "rxjs"; // Chemin à vérifier

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
    private accountService: ClientAccountService,
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

  async openAccount() {
    const isAuthenticated = await firstValueFrom(this.authService.isAuthenticated$);

    if (!isAuthenticated) {
      console.error("Utilisateur non authentifié");
      this.router.navigate(['/login']);
      return;
    }

    this.accountService.getCurrentAccount().subscribe({
      next: (user) => {
        console.log("User profile role :", user.role);
        const target = user.role === 'WORKER' ? '/profile-management' : '/account';
        this.router.navigate([target]);
      },
      error: () => this.router.navigate(['/account'])
    });
  }
}
