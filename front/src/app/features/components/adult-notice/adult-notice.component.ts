import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import {IonButton, IonContent, ModalController} from "@ionic/angular/standalone";
import {LanguageManagerService} from "../../services/language-manager.service";
import {LegalText} from "../../services/common-service";
import {saveAcceptedNotice} from "../../utils/adult-notice";

/** Where visitors who decline are sent. */
export const LEAVE_URL = 'https://www.google.com';

/** First-visit notice: the site is reserved for adults. The visitor accepts or leaves. */
@Component({
  selector: 'app-adult-notice',
  templateUrl: './adult-notice.component.html',
  styleUrls: ['./adult-notice.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [IonContent, IonButton]
})
export class AdultNoticeComponent {
  @Input({ required: true }) text!: LegalText;

  constructor(private modalCtrl: ModalController, protected languageService: LanguageManagerService) {}

  accept() {
    saveAcceptedNotice(this.text);
    this.modalCtrl.dismiss(true);
  }

  leave() {
    window.location.href = LEAVE_URL;
  }
}
