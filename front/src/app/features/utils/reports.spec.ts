import { EMPTY_REPORT_FILTER, filterReports, flattenReports, reportIdOfLog } from './reports';

describe('reports helpers', () => {
  const summary = [
    { workerId: 'w1', workerUsername: 'Luna', workerEmail: 'luna@x', reportCount: 1, reports: [
      { reportId: 1, authorEmail: 'a@x', reportStatus: 'DISMISSED', reason: 'Fake photos', createdAt: '2026-11-01T10:00:00' },
      { reportId: 3, authorEmail: 'b@x', reportStatus: 'PENDING', reason: 'Spam', createdAt: '2026-11-03T10:00:00' }] },
    { workerId: 'w2', workerUsername: 'Mira', workerEmail: 'mira@x', reportCount: 0, reports: [
      { reportId: 2, authorEmail: 'a@x', reportStatus: 'DISMISSED', reason: 'Rude', createdAt: '2026-11-02T10:00:00' }] },
  ];

  it('lists every report newest first, dismissed ones included', () => {
    expect(flattenReports(summary).map(r => r.reportId)).toEqual([3, 2, 1]);
    expect(flattenReports(null)).toEqual([]);
  });

  it('filters by status, by one report, and by text', () => {
    const rows = flattenReports(summary);
    expect(filterReports(rows, { ...EMPTY_REPORT_FILTER, status: 'DISMISSED' }).map(r => r.reportId)).toEqual([2, 1]);
    expect(filterReports(rows, { ...EMPTY_REPORT_FILTER, reportId: 1 }).map(r => r.reportId)).toEqual([1]);
    expect(filterReports(rows, { ...EMPTY_REPORT_FILTER, text: 'mira' }).map(r => r.reportId)).toEqual([2]);
  });

  it('finds the report of a dismissal in the audit log, whatever the JSON storage', () => {
    expect(reportIdOfLog({ actionType: 'DISMISS_REPORT', targetSnapshot: '{"reportId": 7, "reason": "x"}' })).toBe(7);
    expect(reportIdOfLog({ actionType: 'DISMISS_REPORT', targetSnapshot: JSON.stringify('{"reportId": 7}') })).toBe(7);
    expect(reportIdOfLog({ actionType: 'DISMISS_REPORT', targetSnapshot: 'not json' })).toBeNull();
    expect(reportIdOfLog({ actionType: 'DELETE_SERVICE', targetSnapshot: '{"reportId": 7}' })).toBeNull();
  });
});
