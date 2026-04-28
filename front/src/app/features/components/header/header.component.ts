import {Component, EventEmitter, Output} from '@angular/core';
import {Router, RouterLink} from "@angular/router";
import {AuthService} from "../../services/auth.service";
import {AccountService} from "../../services/account.service";
import {CommonModule} from "@angular/common";
import {IonicModule} from "@ionic/angular";

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, IonicModule, RouterLink],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss'],
})
export class HeaderComponent {

// On émet des événements pour que la homepage ouvre ses propres modales
  @Output() loginRequested = new EventEmitter<void>();
  @Output() registerRequested = new EventEmitter<void>();

  constructor(
    public authService: AuthService,
    private accountService: AccountService,
    private router: Router
  ) {}

  onLogout() {
    this.authService.logout().subscribe(() => {
      this.router.navigate(['/']); // Redirection après déconnexion
    });
  }

  /**
   * Logique de redirection dynamique :
   * Si 'WORKER' -> /profile-management
   * Si 'CLIENT' -> /account
   */
  openAccount() {
    this.accountService.getMe().subscribe({
      next: (user) => {
        if (user.role === 'WORKER') {
          this.router.navigate(['/profile-management']);
        } else {
          this.router.navigate(['/account']);
        }
      },
      error: () => this.router.navigate(['/account']) // Fallback
    });
  }
}
