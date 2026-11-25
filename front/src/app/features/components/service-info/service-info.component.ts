import {ChangeDetectionStrategy, Component, Input} from '@angular/core';

import {IonIcon, IonPopover} from '@ionic/angular/standalone';
import {addIcons} from 'ionicons';
import {informationCircleOutline} from 'ionicons/icons';
import {Service} from '../../models/common.model';

let nextId = 0;

/**
 * Shows the (optional) description of a service in a bubble.
 * - Desktop (mouse): on hover.
 * - Mobile / touch: on tap, closed by tapping elsewhere.
 *
 * The projected content (e.g. the service name) is part of the trigger:
 *   <service-info [service]="s">{{ s.name }}</service-info>  -> name + icon are clickable
 *   <service-info [service]="s"></service-info>               -> icon only
 * Without a description, nothing is clickable and no icon is shown.
 */
@Component({
    selector: 'service-info',
    imports: [IonIcon, IonPopover],
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
    <span class="trigger"
      [id]="hasDescription ? triggerId : null"
      [class.interactive]="hasDescription"
      [attr.role]="hasDescription ? 'button' : null"
      [attr.tabindex]="hasDescription ? 0 : null"
      [attr.aria-label]="hasDescription ? service.name : null"
      (click)="hasDescription && $event.stopPropagation()">
      <ng-content></ng-content>
      @if (hasDescription) {
        <ion-icon name="information-circle-outline" class="info-icon"></ion-icon>
      }
    </span>
    
    @if (hasDescription) {
      <ion-popover
        class="service-info-popover"
        [trigger]="triggerId"
        [triggerAction]="triggerAction"
        side="top"
        alignment="center"
        [showBackdrop]="false">
        <ng-template>
          <div class="service-info-body">
            <strong>{{ service.name }}</strong>
            <p>{{ service.description }}</p>
          </div>
        </ng-template>
      </ion-popover>
    }
    `,
    styles: [`
    :host { display: inline-flex; align-items: center; min-width: 0; }

    .trigger {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      min-width: 0;
    }

    .trigger.interactive {
      cursor: pointer;
      border-radius: 6px;
      outline-offset: 2px;
    }

    .info-icon {
      flex-shrink: 0;
      font-size: 1.05em;
      color: var(--text-muted, #8c7d75);
    }

    .trigger.interactive:hover .info-icon { color: var(--text, #1a1714); }

    ion-popover.service-info-popover {
      --width: auto;
      --max-width: min(280px, 90vw);
      --box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
    }

    .service-info-body {
      padding: 10px 14px;
      font-size: 0.85rem;
      line-height: 1.4;
    }

    .service-info-body strong { display: block; margin-bottom: 4px; }
    .service-info-body p { margin: 0; white-space: pre-line; }
  `]
})
export class ServiceInfoComponent {
  @Input({required: true}) service!: Service;

  readonly triggerId = `service-info-${nextId++}`;

  /** Hover when the device has a real mouse, tap otherwise. */
  readonly triggerAction: 'hover' | 'click' =
    typeof window !== 'undefined' && window.matchMedia?.('(hover: hover) and (pointer: fine)').matches
      ? 'hover'
      : 'click';

  constructor() {
    addIcons({informationCircleOutline});
  }

  get hasDescription(): boolean {
    return !!this.service?.description?.trim();
  }
}
