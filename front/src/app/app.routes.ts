import { Routes } from '@angular/router';
import { HomepageComponent } from './features/components/homepage/homepage.component';
import {NgxPayComponent} from "./features/components/ngx-pay/ngx-pay.component";
import {ProfileComponent} from "./features/components/profile/profile.component";
import {detailedProfileResolver} from "./features/resolver/detailed-profile.resolver";
import {AccountComponent} from "./features/components/account/account.component";
import {authGuard} from "./features/guards/auth.guard";
import {galleryResolver} from "./features/resolver/gallery.resolver";
import {ProfileManagementComponent} from "./features/components/profile-management/profile-management.component";

export const routes: Routes = [
  // Gallery resolver loads WorkerGalleryDTOs (with mainThumbUrl) for the homepage
  { path: '',        component: HomepageComponent, resolve: { workers: galleryResolver } },
  { path: 'home',    component: HomepageComponent, resolve: { workers: galleryResolver } },
  { path: 'gallery', component: HomepageComponent, resolve: { workers: galleryResolver } },
  { path: 'profile-management', component: ProfileManagementComponent },
  { path: 'ngx-pay',            component: NgxPayComponent },
  { path: 'account',            component: AccountComponent, canActivate: [authGuard] },

  // Profile uses UUID from queryParam ?id=
  { path: 'profile', component: ProfileComponent, resolve: { profile: detailedProfileResolver } },

  { path: '**', redirectTo: '', pathMatch: 'full' },
];
