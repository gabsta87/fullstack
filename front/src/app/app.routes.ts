import { Routes } from '@angular/router';
import { HomepageComponent } from './features/components/homepage/homepage.component';
import {NgxPayComponent} from "./features/components/ngx-pay/ngx-pay.component";
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

export const routes: Routes = [
  { path: '',        component: HomepageComponent, resolve: { workers: galleryResolver, allServices : servicesResolver} },
  { path: 'home',    component: HomepageComponent, resolve: { workers: galleryResolver, allServices : servicesResolver } },
  { path: 'gallery', component: HomepageComponent, resolve: { workers: galleryResolver, allServices : servicesResolver } },
  { path: 'profile', component: ProfileComponent,  resolve: { profile: profileVisitingResolver }, canActivate: [authGuard] },
  { path: 'ngx-pay', component: NgxPayComponent, canActivate: [authGuard] },
  { path: 'account', component: AccountComponent, resolve: { profile: accountResolver }, canActivate: [authGuard] },
  { path: 'profile-management', component: ProfileManagementComponent, resolve: { profile: profileManagementResolver, services : servicesResolver }, canActivate: [authGuard, workerOnlyGuard] },

  { path: '**', redirectTo: '', pathMatch: 'full' },
];
