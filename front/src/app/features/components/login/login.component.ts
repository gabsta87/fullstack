import { Component } from '@angular/core';
import { Router } from '@angular/router';
import {AuthService} from "../../services/auth.service";
import {FormsModule} from "@angular/forms";
import {NgIf} from "@angular/common";

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  imports: [
    FormsModule,
    NgIf
  ],
  standalone: true
})
export class LoginComponent {
  pseudo = '';
  password = '';
  errorMessage = '';

  constructor(private authService: AuthService, private router: Router) {}

  login(): void {
    this.errorMessage = '';
    this.authService.login(this.pseudo, this.password, this.authService.getRedirectUrl())
      .subscribe({
        next: () => {
          const redirectUrl = this.authService.getRedirectUrl() || '/home';
          this.router.navigateByUrl(redirectUrl);
        },
        error: (err) => {
          this.errorMessage = err.status === 401
            ? 'Identifiants incorrects.'
            : 'Une erreur est survenue. Réessayez.';
        }
      });
  }
}
