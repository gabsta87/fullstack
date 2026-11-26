import { NEVER, of, Subject } from 'rxjs';
import { fakeAsync, tick } from '@angular/core/testing';
import { restoreSession } from './session-restore';

describe('restoreSession', () => {
  it('does not call the API without a token', async () => {
    const check = jasmine.createSpy('check').and.returnValue(of(true));
    await restoreSession(check, false);
    expect(check).not.toHaveBeenCalled();
  });

  it('waits for the session check when it answers in time', fakeAsync(() => {
    const answer = new Subject<boolean>();
    let done = false;
    restoreSession(() => answer, true, 4000).then(() => done = true);
    tick(1000);
    expect(done).toBeFalse();
    answer.next(true);
    tick();
    expect(done).toBeTrue();
  }));

  it('lets the page render when the API never answers', fakeAsync(() => {
    let done = false;
    restoreSession(() => NEVER, true, 4000).then(() => done = true);
    tick(3999);
    expect(done).toBeFalse();
    tick(1);
    expect(done).toBeTrue();
  }));
});
