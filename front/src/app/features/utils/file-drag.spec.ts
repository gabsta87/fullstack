import { isFileDrag, splitUploadable } from './file-drag';

describe('file drag helpers', () => {
  it('only reacts to files coming from the computer', () => {
    expect(isFileDrag({ dataTransfer: { types: ['Files'] } } as unknown as DragEvent)).toBeTrue();
    expect(isFileDrag({ dataTransfer: { types: ['text/plain'] } } as unknown as DragEvent)).toBeFalse();
    expect(isFileDrag({ dataTransfer: null } as unknown as DragEvent)).toBeFalse();
  });

  it('keeps photos and videos, and names the rest', () => {
    const f = (name: string, type: string) => new File(['x'], name, { type });
    const { accepted, refused } = splitUploadable([f('a.jpg', 'image/jpeg'), f('b.pdf', 'application/pdf'), f('c.mov', 'video/quicktime'), f('d.svg', 'image/svg+xml')]);
    expect(accepted.map(x => x.name)).toEqual(['a.jpg', 'c.mov']);
    expect(refused.map(x => x.name)).toEqual(['b.pdf', 'd.svg']);
  });
});
