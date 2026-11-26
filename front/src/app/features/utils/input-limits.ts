/**
 * Client-side mirror of the backend rules (serv/.../common/InputLimits.java and PasswordPolicy.java).
 * The backend remains the authority: these values only give early feedback to the user.
 */
export const InputLimits = {
  PASSWORD_MIN: 8,
  PASSWORD_MAX: 72,
  USERNAME_MAX: 50,
  SHORT_DESCRIPTION_MAX: 45,
  DESCRIPTION_MAX: 5000,
  COMMENT_MAX: 2000,
  REASON_MAX: 1000,
  PHONE_MAX: 20,
} as const;

/** Digits with an optional leading '+', 6 to 20 characters. */
export const PHONE_PATTERN = /^(?=.{6,20}$)\+?[0-9]+$/;

/** Keeps digits and an optional leading '+', as the user types. */
export function sanitizePhone(value: string): string {
  const trimmed = value.trim();
  const plus = trimmed.startsWith('+') ? '+' : '';
  return plus + trimmed.replace(/\D/g, '');
}

/** While typing: digits, an optional leading "+", and single spaces (as in "+41 79 123 45 67"). */
export function sanitizePhoneTyping(value: string): string {
  const plus = value.trimStart().startsWith('+') ? '+' : '';
  return plus + value.replace(/[^\d ]/g, '').replace(/ {2,}/g, ' ').trimStart();
}

export function isValidPhone(value: string | null | undefined): boolean {
  return !value || PHONE_PATTERN.test(value);
}
