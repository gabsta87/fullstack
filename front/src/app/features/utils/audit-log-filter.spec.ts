import { AdminLog } from '../models/common.model';
import { auditLogChoices, EMPTY_AUDIT_LOG_FILTER, filterAuditLogs } from './audit-log-filter';

const log = (over: Partial<AdminLog>): AdminLog => ({
  id: 1, adminId: 'a1', adminName: 'Alice', actionType: 'BAN', createdAt: '2026-11-26T10:00:00', ...over,
});

describe('audit log filter', () => {
  const logs = [
    log({ id: 1, adminId: 'a1', adminName: 'Alice', actionType: 'UPDATE_WORKER_STATUS', targetLabel: 'Luna', targetId: 'w-1' }),
    log({ id: 2, adminId: 'a2', adminName: 'Bob', actionType: 'COMMENT_DELETED', targetSnapshot: '{"workerId":"w-1"}' }),
    log({ id: 3, adminId: 'a2', adminName: 'Bob', actionType: 'CREATE_SERVICE', targetLabel: 'Massage' }),
  ];

  it('keeps everything without a filter', () => {
    expect(filterAuditLogs(logs, EMPTY_AUDIT_LOG_FILTER).length).toBe(3);
  });

  it('filters by admin and by action', () => {
    expect(filterAuditLogs(logs, { ...EMPTY_AUDIT_LOG_FILTER, adminId: 'a2' }).map(l => l.id)).toEqual([2, 3]);
    expect(filterAuditLogs(logs, { ...EMPTY_AUDIT_LOG_FILTER, actionType: 'CREATE_SERVICE' }).map(l => l.id)).toEqual([3]);
  });

  it('finds every action on one target, including deleted items (snapshot)', () => {
    expect(filterAuditLogs(logs, { ...EMPTY_AUDIT_LOG_FILTER, text: 'W-1' }).map(l => l.id)).toEqual([1, 2]);
    expect(filterAuditLogs(logs, { ...EMPTY_AUDIT_LOG_FILTER, text: 'luna' }).map(l => l.id)).toEqual([1]);
  });

  it('lists each admin and action once', () => {
    const choices = auditLogChoices(logs);
    expect(choices.admins).toEqual([{ id: 'a1', name: 'Alice' }, { id: 'a2', name: 'Bob' }]);
    expect(choices.actions).toEqual(['COMMENT_DELETED', 'CREATE_SERVICE', 'UPDATE_WORKER_STATUS']);
  });
});
