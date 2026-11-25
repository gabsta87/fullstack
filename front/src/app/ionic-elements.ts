/**
 * Registers every Ionic web component used in the templates.
 *
 * The app boots Ionic in standalone mode (provideIonicAngular): a tag is only registered when a
 * component imports its standalone class from @ionic/angular/standalone. Components that import the
 * legacy IonicModule register nothing, so a tag only worked if another component happened to import
 * it (e.g. ion-toolbar), and ion-segment / ion-avatar stayed inert (broken tabs).
 * Registering an already registered tag is a no-op.
 *
 * When a new <ion-*> tag is used in a template, add it here (checked by ionic-elements.spec.ts and CI).
 */
import { defineCustomElement as defineIonAvatar } from '@ionic/core/components/ion-avatar.js';
import { defineCustomElement as defineIonBadge } from '@ionic/core/components/ion-badge.js';
import { defineCustomElement as defineIonButton } from '@ionic/core/components/ion-button.js';
import { defineCustomElement as defineIonButtons } from '@ionic/core/components/ion-buttons.js';
import { defineCustomElement as defineIonCard } from '@ionic/core/components/ion-card.js';
import { defineCustomElement as defineIonCardContent } from '@ionic/core/components/ion-card-content.js';
import { defineCustomElement as defineIonCardHeader } from '@ionic/core/components/ion-card-header.js';
import { defineCustomElement as defineIonCardTitle } from '@ionic/core/components/ion-card-title.js';
import { defineCustomElement as defineIonCheckbox } from '@ionic/core/components/ion-checkbox.js';
import { defineCustomElement as defineIonChip } from '@ionic/core/components/ion-chip.js';
import { defineCustomElement as defineIonCol } from '@ionic/core/components/ion-col.js';
import { defineCustomElement as defineIonContent } from '@ionic/core/components/ion-content.js';
import { defineCustomElement as defineIonFab } from '@ionic/core/components/ion-fab.js';
import { defineCustomElement as defineIonFabButton } from '@ionic/core/components/ion-fab-button.js';
import { defineCustomElement as defineIonGrid } from '@ionic/core/components/ion-grid.js';
import { defineCustomElement as defineIonHeader } from '@ionic/core/components/ion-header.js';
import { defineCustomElement as defineIonIcon } from 'ionicons/components/ion-icon.js';
import { defineCustomElement as defineIonInfiniteScroll } from '@ionic/core/components/ion-infinite-scroll.js';
import { defineCustomElement as defineIonInfiniteScrollContent } from '@ionic/core/components/ion-infinite-scroll-content.js';
import { defineCustomElement as defineIonInput } from '@ionic/core/components/ion-input.js';
import { defineCustomElement as defineIonItem } from '@ionic/core/components/ion-item.js';
import { defineCustomElement as defineIonLabel } from '@ionic/core/components/ion-label.js';
import { defineCustomElement as defineIonList } from '@ionic/core/components/ion-list.js';
import { defineCustomElement as defineIonModal } from '@ionic/core/components/ion-modal.js';
import { defineCustomElement as defineIonPopover } from '@ionic/core/components/ion-popover.js';
import { defineCustomElement as defineIonProgressBar } from '@ionic/core/components/ion-progress-bar.js';
import { defineCustomElement as defineIonRange } from '@ionic/core/components/ion-range.js';
import { defineCustomElement as defineIonRefresher } from '@ionic/core/components/ion-refresher.js';
import { defineCustomElement as defineIonRefresherContent } from '@ionic/core/components/ion-refresher-content.js';
import { defineCustomElement as defineIonRippleEffect } from '@ionic/core/components/ion-ripple-effect.js';
import { defineCustomElement as defineIonRow } from '@ionic/core/components/ion-row.js';
import { defineCustomElement as defineIonSegment } from '@ionic/core/components/ion-segment.js';
import { defineCustomElement as defineIonSegmentButton } from '@ionic/core/components/ion-segment-button.js';
import { defineCustomElement as defineIonSelect } from '@ionic/core/components/ion-select.js';
import { defineCustomElement as defineIonSelectOption } from '@ionic/core/components/ion-select-option.js';
import { defineCustomElement as defineIonSpinner } from '@ionic/core/components/ion-spinner.js';
import { defineCustomElement as defineIonText } from '@ionic/core/components/ion-text.js';
import { defineCustomElement as defineIonTextarea } from '@ionic/core/components/ion-textarea.js';
import { defineCustomElement as defineIonTitle } from '@ionic/core/components/ion-title.js';
import { defineCustomElement as defineIonToolbar } from '@ionic/core/components/ion-toolbar.js';

export const IONIC_TAGS: readonly string[] = [
  'ion-avatar',
  'ion-badge',
  'ion-button',
  'ion-buttons',
  'ion-card',
  'ion-card-content',
  'ion-card-header',
  'ion-card-title',
  'ion-checkbox',
  'ion-chip',
  'ion-col',
  'ion-content',
  'ion-fab',
  'ion-fab-button',
  'ion-grid',
  'ion-header',
  'ion-icon',
  'ion-infinite-scroll',
  'ion-infinite-scroll-content',
  'ion-input',
  'ion-item',
  'ion-label',
  'ion-list',
  'ion-modal',
  'ion-popover',
  'ion-progress-bar',
  'ion-range',
  'ion-refresher',
  'ion-refresher-content',
  'ion-ripple-effect',
  'ion-row',
  'ion-segment',
  'ion-segment-button',
  'ion-select',
  'ion-select-option',
  'ion-spinner',
  'ion-text',
  'ion-textarea',
  'ion-title',
  'ion-toolbar',
];

export function registerIonicElements(): void {
  defineIonAvatar();
  defineIonBadge();
  defineIonButton();
  defineIonButtons();
  defineIonCard();
  defineIonCardContent();
  defineIonCardHeader();
  defineIonCardTitle();
  defineIonCheckbox();
  defineIonChip();
  defineIonCol();
  defineIonContent();
  defineIonFab();
  defineIonFabButton();
  defineIonGrid();
  defineIonHeader();
  defineIonIcon();
  defineIonInfiniteScroll();
  defineIonInfiniteScrollContent();
  defineIonInput();
  defineIonItem();
  defineIonLabel();
  defineIonList();
  defineIonModal();
  defineIonPopover();
  defineIonProgressBar();
  defineIonRange();
  defineIonRefresher();
  defineIonRefresherContent();
  defineIonRippleEffect();
  defineIonRow();
  defineIonSegment();
  defineIonSegmentButton();
  defineIonSelect();
  defineIonSelectOption();
  defineIonSpinner();
  defineIonText();
  defineIonTextarea();
  defineIonTitle();
  defineIonToolbar();
}
