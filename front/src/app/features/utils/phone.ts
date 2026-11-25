import { parsePhoneNumberFromString } from 'libphonenumber-js/min';

/** Numbers saved without an international prefix are read as Swiss numbers. */
const DEFAULT_COUNTRY = 'CH';

/** Readable phone number: "+41791234567" → "+41 79 123 45 67". Unknown formats are returned unchanged. */
export function formatPhone(phone: string | null | undefined): string {
  if (!phone) return '';
  const parsed = parsePhoneNumberFromString(phone, DEFAULT_COUNTRY);
  return parsed?.isValid() ? parsed.formatInternational() : phone;
}

/** Digits expected by wa.me links: international number without "+", spaces or leading zeros. */
export function whatsappNumber(phone: string | null | undefined): string {
  if (!phone) return '';
  const parsed = parsePhoneNumberFromString(phone, DEFAULT_COUNTRY);
  return parsed?.isValid() ? parsed.number.replace('+', '') : phone.replace(/\D/g, '');
}
