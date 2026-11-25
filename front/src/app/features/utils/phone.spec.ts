import { formatPhone, whatsappNumber } from './phone';

describe('phone utils', () => {
  it('groups the digits of international numbers', () => {
    expect(formatPhone('+41791234567')).toBe('+41 79 123 45 67');
    expect(formatPhone('+33612345678')).toBe('+33 6 12 34 56 78');
  });

  it('reads national numbers as Swiss', () => {
    expect(formatPhone('0791234567')).toBe('+41 79 123 45 67');
    expect(whatsappNumber('0791234567')).toBe('41791234567');
  });

  it('keeps unknown formats unchanged and never fails', () => {
    expect(formatPhone('12')).toBe('12');
    expect(formatPhone(null)).toBe('');
    expect(whatsappNumber('+41 79 123 45 67')).toBe('41791234567');
  });
});
