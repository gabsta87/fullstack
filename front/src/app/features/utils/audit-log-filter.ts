import { AdminLog } from '../models/common.model';

export interface AuditLogFilter {
  adminId: string | null;
  actionType: string | null;
  /** Matches the target (label, id, snapshot) and the details, case-insensitive. */
  text: string;
}

export const EMPTY_AUDIT_LOG_FILTER: AuditLogFilter = { adminId: null, actionType: null, text: '' };

export function filterAuditLogs(logs: AdminLog[], filter: AuditLogFilter): AdminLog[] {
  const text = filter.text.trim().toLowerCase();
  return logs.filter(log =>
    (!filter.adminId || log.adminId === filter.adminId) &&
    (!filter.actionType || log.actionType === filter.actionType) &&
    (!text || [log.targetLabel, log.targetId, log.targetSnapshot, log.details]
      .some(value => value?.toLowerCase().includes(text))));
}

/** Choices for the admin and action selects, built from the entries themselves. */
export function auditLogChoices(logs: AdminLog[]) {
  const admins = new Map<string, string>();
  logs.forEach(log => admins.set(log.adminId, log.adminName));
  return {
    admins: [...admins].map(([id, name]) => ({ id, name })).sort((a, b) => a.name.localeCompare(b.name)),
    actions: [...new Set(logs.map(log => log.actionType))].sort(),
  };
}
