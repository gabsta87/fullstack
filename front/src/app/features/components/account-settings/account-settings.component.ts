import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {AlertController, IonicModule} from '@ionic/angular';
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
    private accountService: AccountService,
    private alertController: AlertController
  ) {}

  ngOnInit() {
    this.form.email = this.initialEmail;
  }

  async submitForm(t: any) {
    this.errorMessage = '';
    this.successMessage = '';

    if (this.form.password && this.form.password !== this.form.confirmPassword) {
      this.errorMessage = t.auth?.password_mismatch;
      return;
    }

    if (this.form.email !== this.initialEmail) {
      const confirmed = await this.presentEmailChangeConfirmation(t);
      if (!confirmed) {
        return;
      }
    }

    this.executeSave(t);
  }

  private async presentEmailChangeConfirmation(t: any): Promise<boolean> {
    return new Promise(async (resolve) => {
      const alert = await this.alertController.create({
        header: t.account_settings?.email_change_title,
        message: t.account_settings?.email_change_message,
        buttons: [
          {
            text: t.common?.cancel,
            role: 'cancel',
            handler: () => resolve(false)
          },
          {
            text: t.account_settings?.confirm_btn,
            role: 'confirm',
            handler: () => resolve(true)
          }
        ]
      });
      await alert.present();
    });
  }

  private async executeSave(t: any) {
    this.isSaving = true;

    const payload: any = {
      email: this.form.email,
      oldPassword: this.form.oldPassword
    };

    if (this.form.password) {
      payload.password = this.form.password;
    }

    try {
      const response: any = await this.accountService.updateSettings(payload);
      this.successMessage = response?.message || t.account_settings?.success_update;

      if (this.form.email !== this.initialEmail) {
        this.initialEmail = this.form.email;
      }

      this.form.password = '';
      this.form.confirmPassword = '';
      this.form.oldPassword = '';
    } catch (err: any) {
      this.errorMessage = err?.error?.error || err?.error?.message || t.common?.error || 'Error';
    } finally {
      this.isSaving = false;
    }
  }
}
