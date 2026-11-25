import { LanguageManagerService } from './language-manager.service';
import languageData from '../../../assets/lang/data.json';

describe('LanguageManagerService', () => {
  let service: LanguageManagerService;

  beforeEach(() => {
    localStorage.removeItem('site_lang');
    service = new LanguageManagerService();
  });

  afterEach(() => localStorage.removeItem('site_lang'));

  it('translates messages in the current language and fills the parameters', () => {
    expect(service.msg('report_sent')).toBe('Signalement envoyé.');
    service.changeLanguageTo('EN');
    expect(service.msg('admin_not_worker', { role: 'CLIENT' })).toBe('This user is a CLIENT: they have no advertiser profile.');
  });

  it('falls back to English, then to the key', () => {
    service.currentLanguage$.next({ messages: {} });
    expect(service.msg('report_sent')).toBe('Report sent.');
    expect(service.msg('no_such_key')).toBe('no_such_key');
  });

  it('every language defines the same messages and mastery levels as English', () => {
    const data = languageData as any;
    const english = Object.keys(data.en.messages).sort();
    for (const lang of Object.keys(data)) {
      expect(Object.keys(data[lang].messages).sort()).withContext(lang).toEqual(english);
      expect(Object.keys(data[lang].common.mastery_levels).length).withContext(lang).toBe(4);
    }
  });

  it('translates the mastery levels', () => {
    const t = service.currentLanguage$.value;
    expect(service.getMasteryLabel(4, t)).toBe('Langue maternelle');
  });
});
