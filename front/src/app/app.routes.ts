import {Routes} from '@angular/router';
import {HomepageComponent} from './features/components/homepage/homepage.component';
import {ProfileComponent} from "./features/components/profile/profile.component";
import {AccountComponent} from "./features/components/account/account.component";
import {authGuard} from "./features/guards/auth.guard";
import {galleryResolver} from "./features/resolver/gallery.resolver";
import {ProfileManagementComponent} from "./features/components/profile-management/profile-management.component";
import {servicesResolver} from "./features/resolver/services.resolver";
import {profileManagementResolver} from "./features/resolver/profile-management.resolver";
import {accountResolver} from "./features/resolver/account.resolver";
import {workerOnlyGuard} from "./features/guards/worker-only.guard";
import {profileVisitingResolver} from "./features/resolver/profile-visiting.resolver";
import {geographicZonesResolver} from "./features/resolver/geographic-zones.resolver";

export const routes: Routes = [
  {
    path: '',
    resolve: {
      workers: galleryResolver,
      allServices: servicesResolver,
      locations: geographicZonesResolver
    },
    children: [
      { path: 'home', component: HomepageComponent },
      { path: 'gallery', component: HomepageComponent },
      { path: '', redirectTo: 'home', pathMatch: 'full' }
    ]
  },
  { path: 'profile', component: ProfileComponent,
    resolve: {
      profile: profileVisitingResolver,
      locations:    geographicZonesResolver,
  } },
  { path: 'account', component: AccountComponent, canActivate: [authGuard],
    resolve: {
      profile: accountResolver,
      locations:    geographicZonesResolver,
    } },
  { path: 'profile-management', component: ProfileManagementComponent,
    resolve: {
      profile: profileManagementResolver,
      services : servicesResolver,
      locations: geographicZonesResolver,
    }, canActivate: [authGuard, workerOnlyGuard] },

  { path: '**', redirectTo: '', pathMatch: 'full' },
];
