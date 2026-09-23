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
import {logInOutline, logOutOutline, personCircleOutline, buildOutline} from "ionicons/icons";
import LanguageManagerService from "../../services/language-manager.service";

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
    private modalCtrl: ModalController,
    public langService: LanguageManagerService
  ) {
    addIcons({
      personCircleOutline, logOutOutline, logInOutline, buildOutline
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

  async openAdmin(){
    await this.router.navigate(['/admin-dashboard']);
  }

  async openAccount() {
    const isAuthenticated = await firstValueFrom(this.authService.isAuthenticated$);

    if (!isAuthenticated) {
      await this.openAuth('login');
      return;
    }

    await this.router.navigate(['/account-router']);
  }

  changeLanguage(langCode: string) {
    this.langService.changeLanguageTo(langCode);
  }

  private applyLanguage(lang: string) {
    console.log("Application de la langue d'interface :", lang);
    // Lier ici le système de traduction (ex: translateService.use(lang.toLowerCase()))
  }
}
