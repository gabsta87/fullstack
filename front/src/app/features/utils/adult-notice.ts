import { LegalText } from '../services/common-service';

/** What the visitor accepted on this browser: the notice is shown again only when its text changes. */
export const ADULT_NOTICE_STORAGE_KEY = 'adult_notice_accepted';

export interface AcceptedNotice {
  language: string;
  version: number;
}

export function readAcceptedNotice(): AcceptedNotice | null {
  try {
    const value = JSON.parse(localStorage.getItem(ADULT_NOTICE_STORAGE_KEY) || 'null');
    return value && typeof value.language === 'string' && Number.isInteger(value.version) ? value : null;
  } catch {
    return null;
  }
}

export function saveAcceptedNotice(text: LegalText): void {
  try {
    localStorage.setItem(ADULT_NOTICE_STORAGE_KEY, JSON.stringify({ language: text.language, version: text.version }));
  } catch {
    // Storage blocked (private mode): the notice will simply be shown again on the next visit
  }
}

/**
 * Shown on the first visit, and again when a new version of the text the visitor accepted is published.
 * Switching the site language does not show it again.
 */
export function mustShowAdultNotice(accepted: AcceptedNotice | null, text: LegalText): boolean {
  if (!accepted) return true;
  return accepted.language === text.language && accepted.version < text.version;
}
