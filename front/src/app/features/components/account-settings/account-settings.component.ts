import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-account-settings',
  standalone: true,
  imports: [CommonModule, IonicModule, FormsModule],
  templateUrl: './account-settings.component.html',
  styleUrls: ['./account-settings.component.scss']
})
export class AccountSettingsComponent implements OnInit {
  // Données initiales fournies par le parent
  @Input() initialUsername: string = '';
  @Input() initialEmail: string = '';

  // Distinguer s'il s'agit d'un prestataire (Worker) ou non
  @Input() isWorker: boolean = false;
  @Input() subscriptionLabel: string | null = null;
  @Input() subscriptionColor: string | null = 'medium';

  // Événements renvoyés vers le parent pour traitement API
  @Output() onSaveSettings = new EventEmitter<any>();
  @Output() onManageSubscription = new EventEmitter<void>();

  form = {
    username: '',
    email: '',
    password: '',
    confirmPassword: ''
  };

  errorMessage: string = '';
  successMessage: string = '';
  isSaving: boolean = false;

  ngOnInit() {
    this.form.username = this.initialUsername;
    this.form.email = this.initialEmail;
  }

  submitForm() {
    this.errorMessage = '';
    this.successMessage = '';

    // Validation locale basique
    if (this.form.password && this.form.password !== this.form.confirmPassword) {
      this.errorMessage = 'Les mots de passe ne correspondent pas.';
      return;
    }

    this.isSaving = true;

    const payload: any = {
      username: this.form.username,
      email: this.form.email
    };

    if (this.form.password) {
      payload.password = this.form.password;
    }

    // On émet vers le parent avec des callbacks pour piloter l'état visuel du bouton
    this.onSaveSettings.emit({
      payload,
      setLoading: (loading: boolean) => this.isSaving = loading,
      setSuccess: (msg: string) => {
        this.successMessage = msg;
        this.form.password = '';
        this.form.confirmPassword = '';
      },
      setError: (msg: string) => this.errorMessage = msg
    });
  }
}
