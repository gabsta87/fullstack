import { AdminSetting } from '../models/common.model';

/**
 * Same checks as the backend (SettingsService.update): each value is a whole number within its range,
 * and the renewal period stays shorter than the certification validity.
 * Returns the message key of the first problem, or null when the form can be saved.
 */
export function settingsError(settings: AdminSetting[], draft: Record<string, number | null | undefined>): string | null {
  for (const s of settings) {
    const v = draft[s.key];
    if (v === null || v === undefined || !Number.isInteger(v) || v < s.min || v > s.max) return 'admin_settings_range';
  }
  const validity = draft['CERTIFICATION_VALIDITY_MONTHS'];
  const renewal = draft['CERTIFICATION_RENEWAL_MONTHS'];
  if (validity != null && renewal != null && renewal >= validity) return 'admin_settings_renewal_too_long';
  return null;
}

/** Only the values that differ from the stored ones (nothing to send when empty). */
export function changedSettings(settings: AdminSetting[], draft: Record<string, number | null | undefined>): Record<string, number> {
  const changed: Record<string, number> = {};
  for (const s of settings) {
    const v = draft[s.key];
    if (v !== null && v !== undefined && v !== s.value) changed[s.key] = v;
  }
  return changed;
}
