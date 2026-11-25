import {AfterViewInit, Component, Input, OnInit} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {NgIf} from '@angular/common';
import {IonButton, IonSpinner} from '@ionic/angular/standalone';
import {environment} from '../../../../environments/environment';

declare var Stripe: any;

@Component({
  selector: 'stripe-payment',
  standalone : true,
  templateUrl: './stripe-payment.component.html',
  styleUrls: ['./stripe-payment.component.scss'],
  imports: [IonButton, IonSpinner, NgIf],
})
export class StripePaymentComponent implements OnInit, AfterViewInit {
  @Input() workerId!: string;
  @Input() amount!: number;       // In cents (e.g. 1500 for 15.00)
  @Input() paymentType!: 'DAYS' | 'BOOST';

  stripe: any;
  elements: any;
  isProcessing = false;
  private clientSecret: string = '';

  constructor(private http: HttpClient) {}

  ngOnInit() {
    // Initialize Stripe with the public key provided by the API
    this.http.get<{ publicKey: string }>(`${environment.apiBase}/payment/config`).subscribe(config => {
      this.stripe = Stripe(config.publicKey);
      this.createIntent();
    });
  }

  ngAfterViewInit() {
    // The #payment-element container must be rendered.
  }

  private createIntent() {
    const payload = {
      workerId: this.workerId,
      amount: this.amount,
      currency: 'EUR',
      paymentType: this.paymentType
    };

    this.http.post<{ clientSecret: string }>(`${environment.apiBase}/payment/create-payment-intent`, payload)
      .subscribe({
        next: (res) => {
          this.clientSecret = res.clientSecret;
          this.mountStripeForm();
        },
        error: (err) => console.error("Erreur d'initialisation du paiement", err)
      });
  }

  private mountStripeForm() {
    if (!this.stripe || !this.clientSecret) return;

    // Unified payment form (card, Apple Pay, etc.)
    this.elements = this.stripe.elements({ clientSecret: this.clientSecret });
    const paymentElement = this.elements.create('payment', { layout: 'accordion' });
    paymentElement.mount('#payment-element');
  }

  async handleSubmit() {
    if (this.isProcessing || !this.stripe || !this.elements) return;

    this.isProcessing = true;

    // Data goes straight from the browser to Stripe
    const { error } = await this.stripe.confirmPayment({
      elements: this.elements,
      confirmParams: {
        return_url: `${window.location.origin}/profile-management?payment=success`,
      },
    });

    if (error) {
      console.error(error.message);
      alert(`Échec du paiement : ${error.message}`);
      this.isProcessing = false;
    }
    // Without error, Stripe redirects to return_url itself
  }
}
