import { AdminSetting } from '../models/common.model';
import { changedSettings, settingsError } from './settings-form';

describe('settings form', () => {
  const settings: AdminSetting[] = [
    { key: 'CERTIFICATION_VALIDITY_MONTHS', value: 8, defaultValue: 8, min: 1, max: 36 },
    { key: 'CERTIFICATION_RENEWAL_MONTHS', value: 2, defaultValue: 2, min: 0, max: 12 },
    { key: 'STORAGE_LIMIT_MB', value: 50, defaultValue: 50, min: 10, max: 10240 },
  ];
  const draft = (over: Record<string, any> = {}) => ({
    CERTIFICATION_VALIDITY_MONTHS: 8, CERTIFICATION_RENEWAL_MONTHS: 2, STORAGE_LIMIT_MB: 50, ...over });

  it('accepts the stored values', () => {
    expect(settingsError(settings, draft())).toBeNull();
  });

  it('refuses empty, decimal and out-of-range values', () => {
    for (const bad of [null, undefined, 2.5, 0, 37, -1, NaN]) {
      expect(settingsError(settings, draft({ CERTIFICATION_VALIDITY_MONTHS: bad }))).toBe('admin_settings_range');
    }
    expect(settingsError(settings, draft({ STORAGE_LIMIT_MB: 9 }))).toBe('admin_settings_range');
  });

  it('refuses a renewal period as long as the validity', () => {
    expect(settingsError(settings, draft({ CERTIFICATION_VALIDITY_MONTHS: 2 }))).toBe('admin_settings_renewal_too_long');
    expect(settingsError(settings, draft({ CERTIFICATION_RENEWAL_MONTHS: 8 }))).toBe('admin_settings_renewal_too_long');
    expect(settingsError(settings, draft({ CERTIFICATION_VALIDITY_MONTHS: 3 }))).toBeNull();
  });

  it('sends only what changed', () => {
    expect(changedSettings(settings, draft())).toEqual({});
    expect(changedSettings(settings, draft({ STORAGE_LIMIT_MB: 200 }))).toEqual({ STORAGE_LIMIT_MB: 200 });
  });
});
