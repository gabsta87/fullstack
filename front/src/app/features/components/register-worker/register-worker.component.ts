import { Component } from '@angular/core';
import {FormBuilder, FormGroup, Validators, ReactiveFormsModule} from "@angular/forms";
import {RegisterService} from "../../services/register.service";
import {NgIf} from "@angular/common";

@Component({
  selector: 'app-register-worker',
  templateUrl: './register-worker.component.html',
  styleUrls: ['./register-worker.component.scss'],
  imports: [
    ReactiveFormsModule,
    NgIf
  ],
  standalone: true
})
export class RegisterWorkerComponent{

  registerForm: FormGroup;
  isRequesting = false;

  constructor(private fb: FormBuilder, private registerService: RegisterService) {  // Inject the service
    this.registerForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required, this.matchPasswords.bind(this)]],
    });
  }

  matchPasswords(control: any): { [key: string]: boolean } | null {
    if (this.registerForm) {
      return control.value === this.registerForm.get('password')?.value
        ? null
        : { mismatch: true };
    }
    return null;
  }

  onSubmit() {
    if (this.registerForm.valid) {
      this.isRequesting = true;
      const { username, email } = this.registerForm.value;

      // Call the service to register the user (choose client or worker)
      this.registerService.registerWorker(username, email)
        .subscribe({
          next: (response) => {
            console.log('Worker successfully registered:', response);
            alert("Worker successfully registered:")
            this.registerForm.reset();
          },
          error: (err) => {
            console.error('Error registering user:', err);
          }
        });
      this.isRequesting = false;
    } else {
      console.log('Form is invalid');
    }
  }
}
