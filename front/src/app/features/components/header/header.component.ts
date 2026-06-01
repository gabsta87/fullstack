import {Component} from '@angular/core';
import {Router, RouterLink} from "@angular/router";
import {AuthService} from "../../services/auth.service";
import {ClientAccountService} from "../../services/client-account.service";
import {CommonModule} from "@angular/common";
import {IonicModule, ModalController} from "@ionic/angular";
import {AuthModalComponent} from "../auth-modal/auth-modal.component";
import {firstValueFrom} from "rxjs";
import {WorkerAccountService} from "../../services/worker-account.service";

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
    private clientAccountService: ClientAccountService,
    private workerAccountService: WorkerAccountService,
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
  }

  onLogout() {
    this.authService.logout().subscribe(() => {
      this.router.navigate(['/']);
    });
  }

  async openAccount() {
    const isAuthenticated = await firstValueFrom(this.authService.isAuthenticated$);

    if (!isAuthenticated) {
      await this.openAuth('login');
      return;
    }

    // Si on est ici, c'est que l'utilisateur est authentifié
    const user = this.authService.getUser();

    // Navigation basée sur le rôle
    if (user?.role === "WORKER") {
      this.router.navigate(['/profile-management']);
    } else if (user?.role === "CLIENT") {
      this.router.navigate(['/account']);
    } else {
      // Si le rôle est nul (ex: refresh F5), on récupère le profil
      // Note : assurez-vous que votre service met à jour authService.currentAccount
      this.workerAccountService.getCurrentAccount().subscribe({
        next: (profile) => this.router.navigate(['/profile-management']),
        error: () => this.clientAccountService.getCurrentAccount().subscribe({
          next: () => this.router.navigate(['/account']),
          error: () => console.error("Impossible de déterminer le rôle")
        })
      });
    }
  }
}
