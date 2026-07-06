import { Router } from '@angular/router';
import {Component, ElementRef, Input, OnInit, ViewChild} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {AlertController, IonicModule, ModalController} from '@ionic/angular';
import {AuthService} from '../../services/auth.service';
import {RegisterService} from '../../services/register.service';
import {LoadingController} from "@ionic/angular/standalone";
import {ForgotPasswordComponent} from "../forgot-password/forgot-password.component";

@Component({
  selector: 'app-auth-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, IonicModule],
  templateUrl: './auth-modal.component.html',
  styleUrls: ['./auth-modal.component.scss']
})
export class AuthModalComponent implements OnInit {
  @Input() mode: 'login' | 'register' = 'login';
  @ViewChild('input') myInput!: ElementRef;

  authForm!: FormGroup;
  errorMsg = '';
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private alertCtrl: AlertController,
    private loadingCtrl: LoadingController,
    private authService: AuthService,
    private registerService: RegisterService,
    private modalCtrl: ModalController,
    private router: Router
  ) {}

  ngOnInit() {
    this.initForm();
  }

  ngAfterViewInit(){
    setTimeout(() => this.myInput.nativeElement.focus(), 150);
  }

  initForm() {
    if (this.mode === 'register') {
      this.authForm = this.fb.group({
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(6)]],
        confirmPassword: ['', [Validators.required]],
        role: ['client']
      }, { validators: this.passwordMatchValidator });
    } else {
      this.authForm = this.fb.group({
        email: ['', [Validators.required, Validators.email]],
        password: ['', Validators.required]
      });
    }
  }

  passwordMatchValidator(g: FormGroup) {
    return g.get('password')?.value === g.get('confirmPassword')?.value ? null : { mismatch: true };
  }

  switchMode(newMode: 'login' | 'register') {
    this.mode = newMode;
    this.errorMsg = '';
    this.initForm();
  }

  onSubmit() {
    if (this.authForm.invalid) return;

    this.isLoading = true;
    this.errorMsg = '';

    const { password, email, role } = this.authForm.value;

    if (this.mode === 'login') {
      this.authService.login(email, password).subscribe({
        next: (user) => {
          console.log("Connexion réussie !", user);
          this.handleSuccess(user); // 🎯 3. On passe l'utilisateur pour gérer la redirection
        },
        error: (err) => {
          console.error("Erreur reçue du serveur :", err);
          this.isLoading = false;

          if (err.status === 401) {
            this.errorMsg = "Identifiant ou mot de passe incorrects.";
          } else if(err.status == 403){
            this.errorMsg = "Requête interdite.";
          } else {
            this.errorMsg = "Une erreur est survenue lors de la connexion.";
          }
        }
      });
    } else {
      const call = role === 'worker'
        ? this.registerService.registerWorker(email, password)
        : this.registerService.registerClient(email, password);

      call.subscribe({
        next: () => {
          // 🎯 Dès que l'inscription réussit, on se connecte immédiatement en tâche de fond
          this.authService.login(email, password).subscribe({
            next: (loggedInUser) => {
              console.log("Inscription + Connexion automatique réussie !", loggedInUser);
              // On passe le user connecté pour que handleSuccess() fasse la redirection vers le bon espace !
              this.handleSuccess(loggedInUser);
            },
            error: (err) => {
              console.error("Erreur lors de la connexion automatique après inscription :", err);
              this.errorMsg = "Compte créé avec succès, mais la connexion automatique a échoué. Veuillez vous connecter manuellement.";
              this.isLoading = false;
            }
          });
        },
        error: (err) => this.handleError(err)
      });
    }
  }

  // 🎯 5. Nouvelle logique de succès centralisée avec routage intelligent
  private handleSuccess(user: any) {
    this.isLoading = false;
    this.modalCtrl.dismiss(true);

    // On récupère le rôle de l'utilisateur fraîchement connecté
    const userRole = user?.role || this.authService.getUser()?.role;

    if (userRole === 'WORKER') {
      this.router.navigate(['/profile-management']);
    } else if (userRole === 'CLIENT') {
      this.router.navigate(['/account']);
    } else {
      // Par sécurité, si le rôle est indéterminé à cet instant précis
      this.router.navigate(['/']);
    }
  }

  private handleError(err: any) {
    this.isLoading = false;
    this.errorMsg = err.error || "Une erreur est survenue.";
  }

  dismiss() { this.modalCtrl.dismiss(); }

  async goToForgotPassword() {
    const alert = await this.modalCtrl.create(
      {component : ForgotPasswordComponent},
    );

    await alert.present();
  }
}
