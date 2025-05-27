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
    console.log("redirectURL : "+this.authService.getRedirectUrl());
    this.authService.login(this.pseudo, this.password, this.authService.getRedirectUrl()).subscribe(() => {
      console.log("login success");
      const redirectUrl = this.authService.getRedirectUrl() || '/home';
      this.router.navigateByUrl(redirectUrl);
    });
  }
}
