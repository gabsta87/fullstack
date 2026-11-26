export type BodyType = 'SLIM' | 'ATHLETIC' | 'AVERAGE' | 'CURVY' | 'ROUND';
export type EyeColor = 'BROWN' | 'HAZEL' | 'GREEN' | 'BLUE' | 'GREY';
export type HairColor = 'BLACK' | 'BROWN' | 'FAIR' | 'GINGER' | 'BLOND' | 'GREY' | 'COLORED';

export const BODY_TYPES_LIST: BodyType[] = ['SLIM', 'ATHLETIC', 'AVERAGE', 'CURVY', 'ROUND'];
export const EYES_COLOR_LIST: EyeColor[] = ['BROWN', 'HAZEL', 'GREEN', 'BLUE', 'GREY'];
export const HAIR_COLOR_LIST: HairColor[] = ['BLACK', 'BROWN', 'FAIR', 'GINGER', 'BLOND', 'GREY', 'COLORED'];

export interface PhotoItem {
  id: string;
  originalUrl: string;
  mainThumbUrl: string;
  sortOrder : number;
  type?: 'image';
}

export interface VideoItem {
  id: string;
  url: string;
  /** JPEG frame of the video, once processed */
  thumbnailUrl?: string | null;
  /** Seconds */
  duration?: number | null;
  sortOrder : number;
  /** Visitors only receive READY videos; the owner also sees the others */
  status?: 'PROCESSING' | 'READY' | 'FAILED';
  /** VIDEO_UNREADABLE, VIDEO_TOO_LONG or VIDEO_PROCESSING_FAILED */
  failureReason?: string | null;
  type?: 'video';
}

export interface Comment {
  id: number;
  content: string;
  authorId: string;
  workerId: string;
  authorUsername: string;
  date: string;
  /** Commented profile (shown in the client's "My comments") */
  workerUsername?: string;
  workerThumbUrl?: string;
}

export interface AdminWorkerReport{
  workerId: string;
  workerUsername: string;
  workerEmail: string;
  reportCount: number;
  reports: Report[];
}

export interface Report{
  reportId: number;
  authorEmail: string;
  reportStatus : string;
  reason: string;
  createdAt: string;
}
