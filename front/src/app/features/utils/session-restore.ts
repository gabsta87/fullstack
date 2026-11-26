import { Observable, firstValueFrom } from 'rxjs';

/** The first page never waits longer than this for the session check. */
export const SESSION_RESTORE_TIMEOUT_MS = 4000;

/**
 * Startup session restore: waits for the session check, but at most `timeoutMs`. When the API is slow
 * or unreachable the page renders anyway (as a visitor); the check keeps running and the header switches
 * to the logged-in state as soon as it answers.
 */
export function restoreSession(checkSession: () => Observable<boolean>, hasToken: boolean,
                               timeoutMs = SESSION_RESTORE_TIMEOUT_MS): Promise<void> {
  if (!hasToken) return Promise.resolve();
  const check = firstValueFrom(checkSession()).then(() => undefined, () => undefined);
  const deadline = new Promise<void>(resolve => setTimeout(resolve, timeoutMs));
  return Promise.race([check, deadline]);
}
