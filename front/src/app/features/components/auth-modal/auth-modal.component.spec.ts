import { FormBuilder } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { AuthModalComponent } from './auth-modal.component';
import { UserRole } from '../../models/roles';

/** Form logic is tested on the class directly: no Ionic rendering needed. */
describe('AuthModalComponent (form logic)', () => {
  let component: AuthModalComponent;
  let authService: jasmine.SpyObj<any>;
  let registerService: jasmine.SpyObj<any>;
  let modalCtrl: jasmine.SpyObj<any>;
  let router: jasmine.SpyObj<any>;

  function create(mode: 'login' | 'register') {
    authService = jasmine.createSpyObj('AuthService', ['login', 'getUser']);
    registerService = jasmine.createSpyObj('RegisterService', ['registerClient', 'registerWorker']);
    modalCtrl = jasmine.createSpyObj('ModalController', ['dismiss']);
    router = jasmine.createSpyObj('Router', ['navigate']);
    component = new AuthModalComponent(new FormBuilder(), authService, registerService, modalCtrl, router, {} as any);
    component.mode = mode;
    component.ngOnInit();
  }

  describe('register', () => {
    beforeEach(() => create('register'));

    const fill = (email: string, password: string, confirm = password) =>
      component.authForm.patchValue({ email, password, confirmPassword: confirm });

    it('accepts a valid form', () => {
      fill('new@venus.test', 'Correct-Horse-42');
      expect(component.authForm.valid).toBeTrue();
    });

    it('rejects passwords shorter than 8 or longer than 72 characters', () => {
      fill('new@venus.test', '1234567');
      expect(component.authForm.valid).toBeFalse();
      fill('new@venus.test', 'a'.repeat(73));
      expect(component.authForm.valid).toBeFalse();
    });

    it('rejects mismatching confirmation', () => {
      fill('new@venus.test', 'Correct-Horse-42', 'Correct-Horse-43');
      expect(component.authForm.errors?.['mismatch']).toBeTrue();
    });

    it('rejects invalid emails', () => {
      for (const email of ['', 'plain', 'a@']) {
        fill(email, 'Correct-Horse-42');
        expect(component.authForm.valid).withContext(email).toBeFalse();
      }
    });

    it('does not call the server with an invalid form', () => {
      fill('bad', 'short');
      component.onSubmit();
      expect(registerService.registerClient).not.toHaveBeenCalled();
      expect(registerService.registerWorker).not.toHaveBeenCalled();
    });

    it('registers a worker then logs in and opens profile management', () => {
      fill('new@venus.test', 'Correct-Horse-42');
      component.authForm.patchValue({ role: 'worker' });
      registerService.registerWorker.and.returnValue(of({}));
      authService.login.and.returnValue(of({ role: UserRole.WORKER }));
      component.onSubmit();
      expect(registerService.registerWorker).toHaveBeenCalledWith('new@venus.test', 'Correct-Horse-42');
      expect(router.navigate).toHaveBeenCalledWith(['/profile-management']);
    });
  });

  describe('login', () => {
    beforeEach(() => create('login'));

    it('routes each role to its space', () => {
      const cases: [string, string][] = [
        [UserRole.CLIENT, '/account'], [UserRole.WORKER, '/profile-management'],
        [UserRole.ADMIN, '/admin-dashboard'], [UserRole.SUPER_ADMIN, '/admin-dashboard'],
      ];
      for (const [role, target] of cases) {
        component.authForm.patchValue({ email: 'a@venus.test', password: 'x' });
        authService.login.and.returnValue(of({ role }));
        component.onSubmit();
        expect(router.navigate).withContext(role).toHaveBeenCalledWith([target]);
      }
    });

    it('shows a specific message for each server error', () => {
      const cases: [number, string][] = [[401, 'incorrects'], [403, 'interdite'], [429, 'Trop de tentatives'], [500, 'erreur']];
      for (const [status, fragment] of cases) {
        component.authForm.patchValue({ email: 'a@venus.test', password: 'x' });
        authService.login.and.returnValue(throwError(() => ({ status })));
        component.onSubmit();
        expect(component.errorMsg).withContext(String(status)).toContain(fragment);
        expect(component.isLoading).toBeFalse();
      }
    });
  });
});
