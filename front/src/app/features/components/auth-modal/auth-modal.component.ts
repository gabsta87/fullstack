import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { IonicModule, ModalController } from '@ionic/angular';
import { AuthService } from '../../services/auth.service';
import { RegisterService } from '../../services/register.service';

@Component({
  selector: 'app-auth-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, IonicModule],
  templateUrl: './auth-modal.component.html',
  styleUrls: ['./auth-modal.component.scss'] // SCSS partagé
})
export class AuthModalComponent implements OnInit {
  @Input() mode: 'login' | 'register' = 'login';

  authForm!: FormGroup;
  errorMsg = '';
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private registerService: RegisterService,
    private modalCtrl: ModalController,
    private router: Router
  ) {}

  ngOnInit() {
    this.initForm();
  }

  initForm() {
    if (this.mode === 'register') {
      this.authForm = this.fb.group({
        username: ['', [Validators.required, Validators.minLength(3)]],
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(6)]],
        confirmPassword: ['', [Validators.required]],
        role: ['worker']
      }, { validators: this.passwordMatchValidator });
    } else {
      this.authForm = this.fb.group({
        username: ['', Validators.required],
        password: ['', Validators.required]
      });
    }
  }

  passwordMatchValidator(g: FormGroup) {
    return g.get('password')?.value === g.get('confirmPassword')?.value ? null : { mismatch: true };
  }

  // Permet de switcher entre login et register à l'intérieur de la même modale
  switchMode(newMode: 'login' | 'register') {
    this.mode = newMode;
    this.errorMsg = '';
    this.initForm();
  }

  onSubmit() {
    if (this.authForm.invalid) return;
    this.isLoading = true;
    this.errorMsg = '';

    const { username, password, email, role } = this.authForm.value;

    if (this.mode === 'login') {
      this.authService.login(username, password, '').subscribe({
        next: () => this.handleSuccess(),
        error: (err) => this.handleError(err)
      });
    } else {
      const call = role === 'worker'
        ? this.registerService.registerWorker(username, email, password)
        : this.registerService.registerClient(username, email, password);

      call.subscribe({
        next: () => this.authService.login(username, password, '').subscribe(() => this.handleSuccess()),
        error: (err) => this.handleError(err)
      });
    }
  }

  private handleSuccess() {
    this.isLoading = false;
    this.modalCtrl.dismiss(true);
  }

  private handleError(err: any) {
    this.isLoading = false;
    this.errorMsg = err.error || "Une erreur est survenue.";
  }

  dismiss() { this.modalCtrl.dismiss(); }
}
