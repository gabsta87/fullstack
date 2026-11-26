import {Routes} from '@angular/router';
import {HomepageComponent} from './features/components/homepage/homepage.component';
import {ProfileComponent} from "./features/components/profile/profile.component";
import {authGuard} from "./features/guards/auth.guard";
import {galleryResolver} from "./features/resolver/gallery.resolver";
import {servicesResolver} from "./features/resolver/services.resolver";
import {profileManagementResolver} from "./features/resolver/profile-management.resolver";
import {accountResolver} from "./features/resolver/account.resolver";
import {workerOnlyGuard} from "./features/guards/worker-only.guard";
import {profileVisitingResolver} from "./features/resolver/profile-visiting.resolver";
import {geographicZonesResolver} from "./features/resolver/geographic-zones.resolver";
import {ResetPasswordComponent} from "./features/components/reset-password/reset-password.component";
import {adminOnlyGuard} from "./features/guards/admin-only.guard";
import {accountRedirectGuard} from "./features/guards/account-redirect-guard";
import {usersObservableResolver} from "./features/resolver/users-observable-resolver";
import {serviceObservableResolver} from "./features/resolver/service-observable-resolver";
import {geographicZoneObservableResolver} from "./features/resolver/geographic-zone-observable-resolver";
import {logsObservableResolver} from "./features/resolver/logs-observable-resolver";
import {certificationsObservableResolver} from "./features/resolver/certifications-observable-resolver";

export const routes: Routes = [
  {
    path: '',
    resolve: {
      workers:      galleryResolver,
      allServices:  servicesResolver,
      locations:    geographicZonesResolver
    },
    children: [
      { path: 'home', component: HomepageComponent },
      { path: 'gallery', component: HomepageComponent },
      { path: '', redirectTo: 'home', pathMatch: 'full' }
    ]
  },
  { path: 'profile', component: ProfileComponent,
    resolve: {
      profile:      profileVisitingResolver,
      locations:    geographicZonesResolver,
      services:     servicesResolver,
  } },
  { path: 'account',
    // Pages for one role only are loaded on demand: visitors of the gallery do not download them
    loadComponent: () => import('./features/components/account/account.component').then(m => m.AccountComponent), canActivate: [authGuard],
    resolve: {
      profile:      accountResolver,
      locations:    geographicZonesResolver,
    } },
  { path: 'profile-management',
    loadComponent: () => import('./features/components/profile-management/profile-management.component').then(m => m.ProfileManagementComponent),
    resolve: {
      profile:    profileManagementResolver,
      services :  servicesResolver,
      locations:  geographicZonesResolver,
    }, canActivate: [authGuard, workerOnlyGuard] },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'admin-dashboard',
    loadComponent: () => import('./features/components/admin-dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent), canActivate: [authGuard, adminOnlyGuard],
    resolve: {
      users:      usersObservableResolver,
      services :  serviceObservableResolver,
      zones:      geographicZoneObservableResolver,
      logs:       logsObservableResolver,
      certificationRequests: certificationsObservableResolver,
    },
  },
  { path: 'account-router', canActivate: [authGuard, accountRedirectGuard], children: []},

  { path: '**', redirectTo: '', pathMatch: 'full' },
];
