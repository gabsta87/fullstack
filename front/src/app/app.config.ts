import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import {HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi} from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';

import { routes } from './app.routes';
import {provideNgxStripe} from "ngx-stripe";
import { provideIonicAngular } from '@ionic/angular/standalone';
import {JwtInterceptor} from "./features/interceptors/jwt.interceptor";

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(),
    provideNgxStripe(),
    provideAnimations(),
    provideIonicAngular(),
    provideHttpClient(
      withInterceptorsFromDi() // Permet d'utiliser les intercepteurs de type Classe
    ),
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true }
  ]
};
