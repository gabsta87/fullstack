import { Routes } from '@angular/router';
import { HomepageComponent } from './features/components/homepage/homepage.component';
import { RegisterComponent } from './features/components/register-user/register-user.component';
import {NgxPayComponent} from "./features/components/ngx-pay/ngx-pay.component";
import {ProfileComponent} from "./features/components/profile/profile.component";
import {MainGalleryComponent} from "./features/components/main-gallery/main-gallery.component";
import {simpleProfilesResolver} from "./features/resolver/simple-profiles.resolver";
import {detailedProfileResolver} from "./features/resolver/detailed-profile.resolver";
import {RegisterWorkerComponent} from "./features/components/register-worker/register-worker.component";
import {LoginComponent} from "./features/components/login/login.component";
import {AccountComponent} from "./features/components/account/account.component";
import {AuthGuard} from "./features/guards/auth-guard.guard";


export const routes: Routes = [
  { path: '', component: HomepageComponent },  // Default route (Home)
  { path: 'home', component: HomepageComponent },
  { path: 'login', component: LoginComponent },
  { path: 'account', component: AccountComponent, canActivate: [AuthGuard] },
  { path: 'register', component: RegisterComponent },
  { path: 'register-worker', component: RegisterWorkerComponent },
  { path: 'ngx-pay', component: NgxPayComponent },
  { path: 'profile', component: ProfileComponent, resolve:{profile:detailedProfileResolver}  },
  { path: 'gallery', component: MainGalleryComponent, resolve:{profiles:simpleProfilesResolver} },
  { path: '**', redirectTo: '', pathMatch: 'full' } // catch-all
];
