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

/**
 * `messages.*` keys of the profile issues an admin needs to see in the users table.
 * BANNED, LOCKED and HIDDEN are left out: they have their own columns.
 */
export const PROFILE_ISSUE_KEYS: Record<string, string> = {
  USERNAME: 'missing_username', EMAIL: 'missing_email', DESCRIPTION: 'missing_description',
  LOCATION: 'missing_location', PHONE: 'missing_phone', BIRTHDATE: 'missing_birthdate',
  SERVICES: 'missing_service', PHOTOS: 'missing_photo', MINOR: 'issue_minor', EXPIRED: 'subscription_expired',
};

export function profileIssueKeys(issues: string[] | undefined): string[] {
  return (issues ?? []).map(issue => PROFILE_ISSUE_KEYS[issue]).filter((key): key is string => !!key);
}
