import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { switchMap } from 'rxjs/operators';

import { StripeService } from 'ngx-stripe';

@Component({
  selector: 'app-identity',
  standalone: true,
  imports: [],
  templateUrl: './identity.component.html',
  styleUrl: './identity.component.scss'
})
export class IdentityComponent {
  constructor(
    private http: HttpClient,
    private stripeService: StripeService
  ) {}

  verify() {
    // Check the server.js tab to see an example implementation
    this.http.post('/create-verification-session', {})
      .pipe(
        switchMap(session => {
          // Show the verification modal.
          return this.stripeService.verifyIdentity("1")
          // return this.stripeService.verifyIdentity(session.clientSecret)
        })
      )
      .subscribe(result => {
        // If `verifyIdentity` fails, you should display the localized
        // error message to your user using `error.message`.
        if (result.error) {
          alert(result.error.message);
        }
      });
  }
}
