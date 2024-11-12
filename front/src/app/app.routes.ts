import { Routes } from '@angular/router';
import { HomepageComponent } from './features/components/homepage/homepage.component';  // import your Home component
import { RegisterComponent } from './features/components/register-user/register-user.component';
import {NgxPayComponent} from "./features/components/ngx-pay/ngx-pay.component";


export const routes: Routes = [
  { path: '', component: HomepageComponent },  // Default route (Home)
  { path: 'register', component: RegisterComponent },  // Register page route
  { path: 'ngx-pay', component: NgxPayComponent },  // Register page route
];
