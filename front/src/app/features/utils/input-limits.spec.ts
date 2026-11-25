import { InputLimits, isValidPhone, sanitizePhone } from './input-limits';

describe('input limits', () => {
  it('sanitizePhone keeps digits and a leading +', () => {
    expect(sanitizePhone(' +41 79-123.45.67 ')).toBe('+41791234567');
    expect(sanitizePhone('07a9b<script>1')).toBe('0791');
    expect(sanitizePhone('41+79')).toBe('4179');
    expect(sanitizePhone('')).toBe('');
  });

  it('isValidPhone follows the backend pattern', () => {
    for (const ok of ['+41791234567', '0791234567', '123456', '', null, undefined]) {
      expect(isValidPhone(ok as any)).withContext(String(ok)).toBeTrue();
    }
    for (const bad of ['12345', '+41 79', '++4179123', '0'.repeat(21), 'abcdef']) {
      expect(isValidPhone(bad)).withContext(bad).toBeFalse();
    }
  });

  it('mirrors the backend password policy', () => {
    expect(InputLimits.PASSWORD_MIN).toBe(8);
    expect(InputLimits.PASSWORD_MAX).toBe(72);
  });
});
