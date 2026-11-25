import { IONIC_TAGS, registerIonicElements } from './ionic-elements';

describe('Ionic web components', () => {
  it('registers every Ionic tag used in the templates (tabs, avatars... stay functional)', () => {
    registerIonicElements();
    const missing = IONIC_TAGS.filter(tag => !customElements.get(tag));
    expect(missing).toEqual([]);
    expect(customElements.get('ion-segment')).toBeDefined();
    expect(customElements.get('ion-segment-button')).toBeDefined();
  });

  it('can be called twice without error', () => {
    expect(() => { registerIonicElements(); registerIonicElements(); }).not.toThrow();
  });
});
