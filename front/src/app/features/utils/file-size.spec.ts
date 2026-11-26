import { formatFileSize } from './file-size';

describe('formatFileSize', () => {
  const units = { kb: 'Ko', mb: 'Mo' };
  it('shows kilobytes, then megabytes with one decimal, in the site language', () => {
    expect(formatFileSize(300, 'fr', units)).toBe('1 Ko');
    expect(formatFileSize(850 * 1024, 'fr', units)).toBe('850 Ko');
    expect(formatFileSize(1.25 * 1024 * 1024, 'fr', units)).toBe('1,3 Mo');
    expect(formatFileSize(1.25 * 1024 * 1024, 'en', { kb: 'KB', mb: 'MB' })).toBe('1.3 MB');
  });
  it('shows nothing for an unknown size', () => {
    expect(formatFileSize(0, 'fr', units)).toBe('');
    expect(formatFileSize(undefined, 'fr', units)).toBe('');
  });
});
