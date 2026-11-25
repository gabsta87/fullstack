import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';
import { registerIonicElements } from './app/ionic-elements';
import { defineCustomElements } from '@ionic/pwa-elements/loader';

import { register } from 'swiper/element/bundle';
register();

registerIonicElements();

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));

defineCustomElements(window);


