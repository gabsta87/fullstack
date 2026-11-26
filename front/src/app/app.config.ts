import { ApplicationConfig, inject, provideAppInitializer, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import {HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi, withXhr} from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';

import { routes } from './app.routes';
import { provideIonicAngular } from '@ionic/angular/standalone';
import {JwtInterceptor, TOKEN_STORAGE_KEY} from "./features/interceptors/jwt.interceptor";
import {AuthService} from "./features/services/auth.service";
import {restoreSession} from "./features/utils/session-restore";

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideAnimations(),
    provideIonicAngular(),
    provideHttpClient(withXhr(), 
      withInterceptorsFromDi() // Enables class-based interceptors
    ),
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true },
    // Restore the session before the first page renders, whatever page is opened first (a profile link,
    // a reload): without it, public pages showed a logged-in user as a visitor. Bounded wait: a slow API
    // must not leave the site blank.
    provideAppInitializer(() => {
      const auth = inject(AuthService);
      return restoreSession(() => auth.checkSession(), !!localStorage.getItem(TOKEN_STORAGE_KEY));
    }),
  ]
};
