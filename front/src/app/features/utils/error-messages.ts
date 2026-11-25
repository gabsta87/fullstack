/**
 * Maps an HTTP error of `PUT /account/settings` to a `messages.*` translation key.
 * The backend messages are in English and meant for developers: the UI shows its own texts.
 */
export function settingsErrorKey(err: any): string {
  switch (err?.status) {
    case 401: return 'wrong_password';
    case 409: return 'email_taken';
    case 400: return 'register_invalid';
    default: return 'settings_error';
  }
}
