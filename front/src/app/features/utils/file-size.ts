/** "850 Ko" / "1,2 Mo" in the site language; empty for an unknown size (older files). */
export function formatFileSize(bytes: number | null | undefined, locale: string, units: { kb: string; mb: string }): string {
  if (!bytes || bytes <= 0) return '';
  const kb = bytes / 1024;
  if (kb < 1000) {
    return `${new Intl.NumberFormat(locale, { maximumFractionDigits: 0 }).format(Math.max(1, kb))} ${units.kb}`;
  }
  return `${new Intl.NumberFormat(locale, { maximumFractionDigits: 1 }).format(kb / 1024)} ${units.mb}`;
}
