import { Routes } from '@angular/router';
import { HomepageComponent } from './features/components/homepage/homepage.component';
import {NgxPayComponent} from "./features/components/ngx-pay/ngx-pay.component";
import {ProfileComponent} from "./features/components/profile/profile.component";
import {detailedProfileResolver} from "./features/resolver/detailed-profile.resolver";
import {AccountComponent} from "./features/components/account/account.component";
import {authGuard} from "./features/guards/auth.guard";
import {galleryResolver} from "./features/resolver/gallery.resolver";
import {ProfileManagementComponent} from "./features/components/profile-management/profile-management.component";
import {servicesResolver} from "./features/resolver/services.resolver";

export const routes: Routes = [
  { path: '',        component: HomepageComponent, resolve: { workers: galleryResolver, allServices : servicesResolver} },
  { path: 'home',    component: HomepageComponent, resolve: { workers: galleryResolver, allServices : servicesResolver } },
  { path: 'gallery', component: HomepageComponent, resolve: { workers: galleryResolver, allServices : servicesResolver } },
  { path: 'profile', component: ProfileComponent, resolve: { profile: detailedProfileResolver }, canActivate: [authGuard]  },
  { path: 'ngx-pay', component: NgxPayComponent, canActivate: [authGuard] },
  { path: 'account', component: AccountComponent, canActivate: [authGuard] },
  { path: 'profile-management', component: ProfileManagementComponent, canActivate: [authGuard] },

  { path: '**', redirectTo: '', pathMatch: 'full' },
];
