import { AdminWorkerReport, ReportRow } from '../models/items.model';

/** Every report, newest first, with the profile it is about. */
export function flattenReports(summary: AdminWorkerReport[] | null | undefined): ReportRow[] {
  return (summary ?? [])
    .flatMap(w => (w.reports ?? []).map(r => ({
      reportId: r.reportId, workerId: w.workerId, workerUsername: w.workerUsername, workerEmail: w.workerEmail,
      authorEmail: r.authorEmail, reportStatus: r.reportStatus, reason: r.reason, createdAt: r.createdAt,
    })))
    .sort((a, b) => (b.createdAt ?? '').localeCompare(a.createdAt ?? ''));
}

export interface ReportFilter {
  status: 'PENDING' | 'DISMISSED' | null;
  /** One report only (opened from the audit log) */
  reportId: number | null;
  text: string;
}

export const EMPTY_REPORT_FILTER: ReportFilter = { status: null, reportId: null, text: '' };

export function filterReports(rows: ReportRow[], f: ReportFilter): ReportRow[] {
  const text = f.text.trim().toLowerCase();
  return rows.filter(r =>
    (f.reportId === null || r.reportId === f.reportId)
    && (f.status === null || r.reportStatus === f.status)
    && (!text || [r.workerUsername, r.workerEmail, r.authorEmail, r.reason, String(r.reportId)]
          .some(v => (v ?? '').toLowerCase().includes(text))));
}

/** Report id kept in the audit entry of a dismissed report (JSON snapshot), if any. */
export function reportIdOfLog(log: { actionType?: string; targetSnapshot?: string | null }): number | null {
  if (log.actionType !== 'DISMISS_REPORT' || !log.targetSnapshot) return null;
  try {
    let value: unknown = JSON.parse(log.targetSnapshot);
    if (typeof value === 'string') value = JSON.parse(value); // stored as a JSON string by some databases
    const id = (value as { reportId?: unknown })?.reportId;
    return typeof id === 'number' ? id : null;
  } catch {
    return null;
  }
}
