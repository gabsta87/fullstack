import {Component, inject, OnInit, signal, ViewChild} from '@angular/core';
import {ReactiveFormsModule, UntypedFormBuilder, Validators} from '@angular/forms';

import {MatInputModule} from '@angular/material/input';

import {injectStripe, StripeElementsDirective, StripePaymentElementComponent} from 'ngx-stripe';
import {StripeElementsOptions, StripePaymentElementOptions} from '@stripe/stripe-js';
import {environment} from "../../../../environments/environment";
import {HttpClient} from "@angular/common/http";
import {NgIf} from "@angular/common";
import {firstValueFrom} from "rxjs";

@Component({
  selector: 'app-ngx-pay',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatInputModule,
    StripePaymentElementComponent,
    StripeElementsDirective,
    NgIf,
  ],
  templateUrl: './ngx-pay.component.html',
  styleUrl: './ngx-pay.component.scss'
})
export class NgxPayComponent implements OnInit {
  @ViewChild(StripePaymentElementComponent)
  paymentElement!: StripePaymentElementComponent;
  clientSecret!: string;

  constructor(private http: HttpClient) {}

  private readonly fb = inject(UntypedFormBuilder);

  ngOnInit(): void {
    this.createPaymentIntent();
  }

  async createPaymentIntent() {
    console.log("creating payment intent")
    try {
      const response = await firstValueFrom(
        this.http.post<{ clientSecret: string }>('http://localhost:8080/payment/create-payment-intent', {
          billValue: 2500,
          currency: 'USD',
        })
      );

      console.log("response: ", response);
      this.clientSecret = response.clientSecret;
      this.updateElementsOptions();
    } catch (error) {
      console.error('Payment Intent creation failed', error);
    }
  }

  updateElementsOptions() {
    this.elementsOptions = {
      clientSecret: this.clientSecret, // Dynamically set the clientSecret
      locale: 'en',
      appearance: { theme: 'flat' }
    };
  }

  paymentElementForm = this.fb.group({
    name: ['John Doe', [Validators.required]],
    email: ['support@ngx-stripe.dev', [Validators.required]],
    address: [''],
    zipcode: [''],
    city: [''],
    amount: [2500, [Validators.required, Validators.pattern(/\d+/)]]
  });

  elementsOptions: StripeElementsOptions = {
    locale: 'en',
    clientSecret: this.clientSecret,
    appearance: {
      theme: 'flat'
    }
  };

  paymentElementOptions: StripePaymentElementOptions = {
    layout: "accordion"
  };

  // Replace with your own public key
  stripe = injectStripe(environment.STRIPE_PUBLIC_KEY);
  paying = signal(false);

  async pay() {
    console.log("paying")
    if (this.paying() || this.paymentElementForm.invalid || !this.paymentElement.elements) {
      console.error("Payment element not initialized or form invalid");
      return;
    }

    this.paying.set(true);

    const {name, email, address, zipcode, city} = this.paymentElementForm.getRawValue();
    console.log("name: ",name, ", email: ", email, ", address: ", address, ", zipcode: ",zipcode, ", city: "+ city);

    console.log("payement elements : ",this.paymentElement);
    console.log("stripe : ",this.stripe);

    const response = await firstValueFrom(this.stripe
      .confirmPayment({
        elements: this.paymentElement.elements,
        confirmParams: {
          payment_method_data: {
            billing_details: {
              name: name as string,
              email: email as string,
              address: {
                line1: address as string,
                postal_code: zipcode as string,
                city: city as string
              }
            }
          }
        },
        redirect: 'if_required'
      }));

    this.paying.set(false);
    console.log("payment finishing, parsing response",response)

    if (response.error) {
      console.log("error", response.error);
      // Show error to your customer (e.g., insufficient funds)
      alert({success: false, error: response.error.message});
    } else {
      // The payment has been processed!
      if (response.paymentIntent.status === 'succeeded') {
        console.log("successful payment")
        // Show a success message to your customer
        alert({success: true});
      }
    }

  }
}
