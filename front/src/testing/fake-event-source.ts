/** Replaces the browser EventSource in tests, so no real SSE connection is opened. */
export class FakeEventSource {
  static instances: FakeEventSource[] = [];
  listeners: Record<string, ((e: MessageEvent) => void)[]> = {};
  onerror: (() => void) | null = null;
  closed = false;

  constructor(public url: string) {
    FakeEventSource.instances.push(this);
  }

  addEventListener(type: string, cb: (e: MessageEvent) => void) {
    (this.listeners[type] ??= []).push(cb);
  }

  emit(type: string, data: unknown) {
    (this.listeners[type] ?? []).forEach(cb => cb({ data: JSON.stringify(data) } as MessageEvent));
  }

  close() {
    this.closed = true;
  }

  static install(): () => void {
    const original = (window as any).EventSource;
    FakeEventSource.instances = [];
    (window as any).EventSource = FakeEventSource;
    return () => ((window as any).EventSource = original);
  }
}
