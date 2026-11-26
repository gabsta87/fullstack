/** True when the drag carries files from the computer (not text, links or a photo moved inside the page). */
export function isFileDrag(event: DragEvent): boolean {
  return !!event.dataTransfer && Array.from(event.dataTransfer.types || []).includes('Files');
}

export const ALLOWED_UPLOAD_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'video/mp4', 'video/quicktime', 'video/webm'];

/** Splits dropped files into the ones the server accepts and the others (named in a message). */
export function splitUploadable(files: File[]): { accepted: File[]; refused: File[] } {
  const accepted: File[] = [];
  const refused: File[] = [];
  for (const f of files) (ALLOWED_UPLOAD_TYPES.includes(f.type) ? accepted : refused).push(f);
  return { accepted, refused };
}
