import { Component } from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import { RegisterService } from '../../services/register.service';
import {NgIf} from "@angular/common";

@Component({
  selector: 'app-register-user',
  standalone: true,
  templateUrl: './register-user.component.html',
  styleUrls: ['./register-user.component.scss'],
  imports: [
    ReactiveFormsModule,
    NgIf
  ],
})
export class RegisterComponent {
  registerForm: FormGroup;

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
      const { username, email, password} = this.registerForm.value;

      // Call the service to register the user (choose client or worker)
      this.registerService.registerClient(username, email, password)
        .subscribe({
          next: (response) => {
            console.log('User successfully registered:', response);
          },
          error: (err) => {
            console.error('Error registering user:', err);
          }
        });
    } else {
      console.log('Form is invalid');
    }
  }
}
