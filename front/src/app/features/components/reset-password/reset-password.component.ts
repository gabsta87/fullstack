import {Component, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {IonText, ModalController} from '@ionic/angular/standalone';
import {IonButton, IonContent, IonIcon, IonInput, IonItem, IonSpinner} from "@ionic/angular/standalone";
import {AsyncPipe, NgIf} from "@angular/common";
import {AuthService} from "../../services/auth.service";
import {ForgotPasswordComponent} from "../forgot-password/forgot-password.component";
import {LanguageManagerService} from "../../services/language-manager.service";

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.scss'],
  imports: [
    IonContent,
    ReactiveFormsModule,
    IonItem,
    IonInput,
    IonButton,
    IonSpinner,
    IonIcon,
    NgIf,
    AsyncPipe,
    IonText
  ],
  standalone: true
})
export class ResetPasswordComponent implements OnInit {
  resetForm!: FormGroup;
  token: string | null = null;
  isLoading = false;
  isSuccess = false;
  errorMsg = '';

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private authService: AuthService,
    private modalCtrl: ModalController,
    private router : Router,
    protected languageService : LanguageManagerService,
  ) {}

  ngOnInit() {
    this.token = this.route.snapshot.queryParamMap.get('resetToken');

    this.resetForm = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(g: FormGroup) {
    return g.get('newPassword')?.value === g.get('confirmPassword')?.value ? null : { mismatch: true };
  }

  onSubmit() {
    if (this.resetForm.invalid || !this.token) return;

    this.isLoading = true;
    this.errorMsg = '';

    this.authService.confirmPasswordReset(this.token,this.resetForm.value.newPassword).subscribe({
      next: () => {
        this.isLoading = false;
        this.isSuccess = true;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMsg = err.error || "Une erreur est survenue.";
      }
    });
  }

  async goToForgotPassword() {
    const alert = await this.modalCtrl.create(
      {component : ForgotPasswordComponent},
    );

    await alert.present();
  }

  goToRoute(routePath: string) {
    console.log('Navigation vers :', routePath);
    this.router.navigateByUrl(routePath);
  }

}
