import { Router } from '@angular/router';
import {InputLimits} from "../../utils/input-limits";
import {Component, ElementRef, Input, OnInit, ViewChild, ChangeDetectionStrategy} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {AlertController, IonicModule, ModalController} from '@ionic/angular';
import {AuthService} from '../../services/auth.service';
import {RegisterService} from '../../services/register.service';
import {LoadingController} from "@ionic/angular/standalone";
import {ForgotPasswordComponent} from "../forgot-password/forgot-password.component";
import {UserRole} from "../../models/roles";
import {LanguageManagerService} from "../../services/language-manager.service";

@Component({
    selector: 'app-auth-modal',
    imports: [CommonModule, ReactiveFormsModule, FormsModule, IonicModule],
    templateUrl: './auth-modal.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
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
    private authService: AuthService,
    private registerService: RegisterService,
    private modalCtrl: ModalController,
    private router: Router,
    protected languageService : LanguageManagerService,
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
        password: ['', [Validators.required, Validators.minLength(InputLimits.PASSWORD_MIN), Validators.maxLength(InputLimits.PASSWORD_MAX)]],
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
          this.handleSuccess(user); // Pass the user to pick the redirection
        },
        error: (err) => {
          console.error("Login failed", err);
          this.isLoading = false;

          if (err.status === 401) {
            this.errorMsg = this.languageService.msg('login_invalid');
          } else if(err.status == 403){
            this.errorMsg = this.languageService.msg('login_forbidden');
          } else if (err.status === 429) {
            this.errorMsg = this.languageService.msg('login_too_many');
          } else {
            this.errorMsg = this.languageService.msg('login_failed');
          }
        }
      });
    } else {
      const call = role === 'worker'
        ? this.registerService.registerWorker(email, password)
        : this.registerService.registerClient(email, password);

      call.subscribe({
        next: () => {
          // Once registered, log in right away
          this.authService.login(email, password).subscribe({
            next: (loggedInUser) => {
              // handleSuccess() redirects to the user's space
              this.handleSuccess(loggedInUser);
            },
            error: (err) => {
              console.error("Automatic login after registration failed", err);
              this.errorMsg = this.languageService.msg('register_autologin_failed');
              this.isLoading = false;
            }
          });
        },
        error: (err) => this.handleError(err)
      });
    }
  }

  // Success: redirect according to the role
  private handleSuccess(user: any) {
    this.isLoading = false;
    this.modalCtrl.dismiss(true);

    // Role of the freshly logged-in user
    const userRole = user?.role || this.authService.getUser()?.role;

    if (userRole === UserRole.WORKER) {
      this.router.navigate(['/profile-management']);
    } else if (userRole === UserRole.CLIENT) {
      this.router.navigate(['/account']);
    } else if (userRole === UserRole.ADMIN || userRole === UserRole.SUPER_ADMIN) {
      this.router.navigate(['/admin-dashboard']);
    } else {
      // Role unknown at this point: go home
      this.router.navigate(['/']);
    }
  }

  private handleError(err: any) {
    this.isLoading = false;
    this.errorMsg = this.languageService.msg(err?.status === 409 ? 'email_taken'
      : err?.status === 400 ? 'register_invalid'
      : err?.status === 429 ? 'login_too_many'
      : 'generic_error');
  }

  dismiss() { this.modalCtrl.dismiss(); }

  async goToForgotPassword() {
    const alert = await this.modalCtrl.create(
      {component : ForgotPasswordComponent},
    );

    await alert.present();
  }
}
