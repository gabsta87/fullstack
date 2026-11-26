import { Component, Input, Output, EventEmitter, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import {AlertController, IonicModule} from '@ionic/angular';
import { FormsModule } from '@angular/forms';
import {LanguageManagerService} from "../../services/language-manager.service";
import {AccountService} from "../../services/account-service";
@Component({
    selector: 'account-settings',
    imports: [CommonModule, IonicModule, FormsModule],
    templateUrl: './account-settings.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrls: ['./account-settings.component.scss']
})
export class AccountSettingsComponent implements OnInit {
  @Input() initialEmail: string = '';
  /** Visitors only: the name that signs their reviews (no password needed to change it). */
  @Input() initialUsername: string | null = null;

  username = '';
  usernameSaving = false;
  usernameMessage: { ok: boolean; text: string } | null = null;

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
    this.username = this.initialUsername ?? '';
  }

  get usernameChanged(): boolean {
    return this.username.trim() !== (this.initialUsername ?? '').trim();
  }

  /** Server rules: 50 characters at most, not another account's name (409). Empty gives a new pseudonym. */
  async saveUsername(t: any) {
    if (this.usernameSaving || !this.usernameChanged) return;
    this.usernameSaving = true;
    this.usernameMessage = null;
    try {
      const updated: any = await this.accountService.updateSettings({ username: this.username.trim() });
      this.initialUsername = updated?.username ?? this.username.trim();
      this.username = this.initialUsername ?? '';
      this.usernameMessage = { ok: true, text: t.account_settings?.username_saved };
    } catch (err: any) {
      this.usernameMessage = { ok: false, text: err?.status === 409
        ? t.account_settings?.username_taken
        : (err?.status === 400 ? t.account_settings?.username_invalid : (t.common?.error || 'Error')) };
    } finally {
      this.usernameSaving = false;
    }
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
