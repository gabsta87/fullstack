import {Component, ChangeDetectionStrategy, OnInit} from '@angular/core';
import {RouterOutlet} from '@angular/router';
import {ModalController} from "@ionic/angular/standalone";
import {firstValueFrom} from "rxjs";
import {CommonService} from "./features/services/common-service";
import {LanguageManagerService} from "./features/services/language-manager.service";
import {AdultNoticeComponent} from "./features/components/adult-notice/adult-notice.component";
import {mustShowAdultNotice, readAcceptedNotice} from "./features/utils/adult-notice";

@Component({
    selector: 'app-root',
    imports: [RouterOutlet],
    templateUrl: './app.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {

  constructor(private commonService: CommonService,
              private languageService: LanguageManagerService,
              private modalCtrl: ModalController) {}

  ngOnInit() {
    void this.showAdultNoticeIfNeeded();
  }

  /** The site is reserved for adults: the legal notice is accepted before browsing. */
  private async showAdultNoticeIfNeeded() {
    try {
      const text = await firstValueFrom(
        this.commonService.getLegalText('ADULT_NOTICE', this.languageService.currentCode.toLowerCase()));
      if (!mustShowAdultNotice(readAcceptedNotice(), text)) return;

      const modal = await this.modalCtrl.create({
        component: AdultNoticeComponent,
        componentProps: { text },
        backdropDismiss: false,
        keyboardClose: false,
        canDismiss: async (_data, role) => role !== 'backdrop' && role !== 'gesture',
        cssClass: 'adult-notice-modal',
      });
      await modal.present();
    } catch (err) {
      console.warn('Could not load the adult notice', err);
    }
  }
}
