import { ADULT_NOTICE_STORAGE_KEY, mustShowAdultNotice, readAcceptedNotice, saveAcceptedNotice } from './adult-notice';

describe('adult notice', () => {
  const text = (language: string, version: number) => ({ key: 'ADULT_NOTICE', language, version, content: '' });

  afterEach(() => localStorage.removeItem(ADULT_NOTICE_STORAGE_KEY));

  it('is shown on the first visit, then only for a new version of the accepted text', () => {
    expect(mustShowAdultNotice(null, text('fr', 1))).toBeTrue();
    saveAcceptedNotice(text('fr', 1));
    const accepted = readAcceptedNotice();
    expect(mustShowAdultNotice(accepted, text('fr', 1))).toBeFalse();
    expect(mustShowAdultNotice(accepted, text('fr', 2))).toBeTrue();
    expect(mustShowAdultNotice(accepted, text('de', 3))).toBeFalse();
  });

  it('ignores a tampered or corrupted stored value', () => {
    localStorage.setItem(ADULT_NOTICE_STORAGE_KEY, '{not json');
    expect(readAcceptedNotice()).toBeNull();
    localStorage.setItem(ADULT_NOTICE_STORAGE_KEY, JSON.stringify({ language: 'fr', version: 'x' }));
    expect(readAcceptedNotice()).toBeNull();
  });
});
