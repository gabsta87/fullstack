import {Component} from '@angular/core';
import {Router, RouterLink} from "@angular/router";
import {AuthService} from "../../services/auth.service";
import {ClientAccountService} from "../../services/client-account.service";
import {CommonModule} from "@angular/common";
import {IonicModule, ModalController} from "@ionic/angular";
import {AuthModalComponent} from "../auth-modal/auth-modal.component";
import {firstValueFrom} from "rxjs";
import {WorkerAccountService} from "../../services/worker-account.service";
import {addIcons} from "ionicons";
import {
  bodyOutline,
  calendarOutline,
  callOutline, chevronBackOutline, chevronForwardOutline, closeOutline,
  heart,
  heartOutline, locationOutline,
  logoWhatsapp,
  notifications,
  notificationsOutline, personCircleOutline, playCircleOutline, timeOutline,
  warningOutline
} from "ionicons/icons";

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
  ) {
    addIcons({
      personCircleOutline
    });
  }

  ngOnInit() {
    // Au démarrage, on récupère la langue stockée ou on met 'FR' par défaut
    const savedLang = localStorage.getItem('site_lang') || 'FR';
    this.applyLanguage(savedLang);
  }

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

  changeLanguage(lang: string) {
    localStorage.setItem('site_lang', lang);
    this.applyLanguage(lang);
  }

  private applyLanguage(lang: string) {
    console.log("Application de la langue d'interface :", lang);
    // Lier ici le système de traduction (ex: translateService.use(lang.toLowerCase()))
  }
}
