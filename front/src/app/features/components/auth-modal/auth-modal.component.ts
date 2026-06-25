import {Component, ElementRef, Input, OnInit, ViewChild} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {IonicModule, ModalController} from '@ionic/angular';
import {AuthService} from '../../services/auth.service';
import {RegisterService} from '../../services/register.service';

@Component({
  selector: 'app-auth-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, IonicModule],
  templateUrl: './auth-modal.component.html',
  styleUrls: ['./auth-modal.component.scss'] // SCSS partagé
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
  ) {}

  ngOnInit() {
    this.initForm();
  }

  ngAfterViewInit(){
    setTimeout(() => this.myInput.nativeElement.focus(),150);
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
        email: ['', Validators.required, Validators.email],
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

    const { password, email, role } = this.authForm.value;

    if (this.mode === 'login') {
      this.authService.login(email, password).subscribe({
        next: (user) => {
          console.log("Connexion réussie !", user);
          this.isLoading = false;
          this.modalCtrl.dismiss(true);
        },
        error: (err) => {
          console.error("Erreur reçue du serveur :", err);

          this.isLoading = false;

          if (err.status === 401) {
            this.errorMsg = "Identifiants ou mot de passe incorrects.";
          } else if(err.status == 403){
            this.errorMsg = "Requête interdite.";
          }else{
            this.errorMsg = "Une erreur est survenue lors de la connexion.";
          }
        }
      });
    } else {
      const call = role === 'worker'
        ? this.registerService.registerWorker(email, password)
        : this.registerService.registerClient(email, password);

      call.subscribe({
        next: () => this.authService.login(email, password).subscribe(() => this.handleSuccess()),
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
