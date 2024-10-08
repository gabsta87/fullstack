import { Routes } from '@angular/router';
import { HomepageComponent } from './features/components/homepage/homepage.component';  // import your Home component
import { RegisterComponent } from './features/components/register-user/register-user.component';
import {CheckoutComponent} from "./features/components/checkout/checkout.component";  // import your Register component


export const routes: Routes = [
  { path: '', component: HomepageComponent },  // Default route (Home)
  { path: 'register', component: RegisterComponent },  // Register page route
  { path: 'pay', component: CheckoutComponent },  // Register page route
];
