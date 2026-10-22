import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { FormsModule } from '@angular/forms';
import {LanguageManagerService} from "../../services/language-manager.service";
import {AccountService} from "../../services/account-service";

@Component({
  selector: 'account-settings',
  standalone: true,
  imports: [CommonModule, IonicModule, FormsModule],
  templateUrl: './account-settings.component.html',
  styleUrls: ['./account-settings.component.scss']
})
export class AccountSettingsComponent implements OnInit {
  @Input() initialEmail: string = '';

  form = {
    email: '',
    password: '',
    confirmPassword: '',
    oldPassword: ''
  };

  errorMessage: string = '';
  successMessage: string = '';
  isSaving: boolean = false;

  constructor(
    protected languageService: LanguageManagerService,
    private accountService: AccountService // 👈 Ton service injecté ici
  ) {}

  ngOnInit() {
    this.form.email = this.initialEmail;
  }

  async submitForm() {
    this.errorMessage = '';
    this.successMessage = '';

    if (this.form.password && this.form.password !== this.form.confirmPassword) {
      this.errorMessage = 'Les mots de passe ne correspondent pas.';
      return;
    }

    this.isSaving = true;

    const payload: any = {
      email: this.form.email,
      oldPassword: this.form.oldPassword // 👈 Ne pas l'oublier pour le back !
    };

    if (this.form.password) {
      payload.password = this.form.password;
    }

    try {
      await this.accountService.updateSettings(payload);
      this.successMessage = 'Vos paramètres de compte ont été mis à jour avec succès.';
      this.form.password = '';
      this.form.confirmPassword = '';
      this.form.oldPassword = '';
    } catch (err: any) {
      this.errorMessage = err?.error?.error || err?.error?.message || 'Une erreur est survenue.';
    } finally {
      this.isSaving = false;
    }
  }
}
