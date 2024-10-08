// checkout.component.ts
import { Component, OnInit } from '@angular/core';
import { loadStripe, Stripe } from '@stripe/stripe-js';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { CommonModule } from '@angular/common';
import {FormsModule} from "@angular/forms";
import {BehaviorSubject} from "rxjs"; // Import CommonModule for *ngIf


@Component({
  selector: 'app-checkout',
  templateUrl: './checkout.component.html',
  standalone: true,
  styleUrls: ['./checkout.component.scss'],
  imports: [CommonModule, FormsModule]
})
export class CheckoutComponent implements OnInit {
  stripe: Stripe | null = null;
  cardElement: any;

  constructor(private http: HttpClient) {}

  payment = {
    openStripe: false,
    openSuccess: false,
  };

  paymentDetails = {
    email: '',
    amount: 12.00, // Set the amount dynamically
    country: 'Switzerland' // Default country
  };

  clientSecret!: string;
  elements: any;

  async ngOnInit() {
    this.stripe = await loadStripe('pk_test_51Q7G3pP6btVls6Oy8q6RHbIJmCFGfeM6GeJNmE1KG4WyKD0FV0f1ItgovcbDb2M3G51vy6XnHl9S2PCiEsm4wL2N00ZjbRDTsk');
  }

  initializeStripeElements() {
    const appearance = {
      theme: 'stripe' as 'stripe' | 'night' | 'flat', // Assign a valid theme value
    };

    if (!this.stripe) {
      console.error('Stripe failed to initialize');
      return;
    }

    this.elements = this.stripe.elements({
      clientSecret: this.clientSecret,
      appearance
    });

    const paymentElement = this.elements.create('payment');
    paymentElement.mount('#payment-element');
  }

  async onSubmit() {
    if (!this.stripe || !this.cardElement) {
      return;
    }

    const { token, error } = await this.stripe.createToken(this.cardElement);

    if (error) {
      console.error(error);
    } else {
      console.log('Success! Token created:', token);

      // Call backend to create a payment intent and get client secret
      this.http.post<any>(`${environment.apiUrl}/payment/create-payment-intent`, {})
        .subscribe((data) => {
          this.clientSecret = data.client_secret;
          this.initializeStripeElements();
        });

      const elements = this.stripe!.elements();
      this.cardElement = elements.create('card');
      this.cardElement.mount('#card-element');

      this.payment.openSuccess = true;
      this.payment.openStripe = false;
      // You can send the token to your backend to complete the charge
    }
  }

  startPayment(){
    this.payment.openStripe = true;
  }
}

//
//   async makePayment() {
//
//     if (!this.stripe) {
//       console.error('Stripe failed to initialize');
//       return;
//     }
//
//     const { error } = await this.stripe.confirmPayment({
//       elements: this.elements,
//       confirmParams: {
//         return_url: `${window.location.origin}/payment-success`
//       }
//     });
//
//     if (error) {
//       console.error('Payment failed', error.message);
//       // Display an error message to the user
//     } else {
//       // Success
//       window.location.href = '/payment-success';
//     }
//   }
