import { profileIssueKeys, settingsErrorKey } from './error-messages';

describe('settingsErrorKey', () => {
  it('maps the statuses returned by the settings endpoint', () => {
    expect(settingsErrorKey({ status: 401 })).toBe('wrong_password');
    expect(settingsErrorKey({ status: 409 })).toBe('email_taken');
    expect(settingsErrorKey({ status: 400 })).toBe('register_invalid');
  });

  it('falls back to a generic message', () => {
    expect(settingsErrorKey({ status: 500 })).toBe('settings_error');
    expect(settingsErrorKey(undefined)).toBe('settings_error');
  });
});

describe('profileIssueKeys', () => {
  it('maps the issues to translation keys, skipping the ones shown in their own column', () => {
    expect(profileIssueKeys(['PHONE', 'BANNED', 'MINOR', 'HIDDEN', 'UNKNOWN'])).toEqual(['missing_phone', 'issue_minor']);
    expect(profileIssueKeys(undefined)).toEqual([]);
  });
});
