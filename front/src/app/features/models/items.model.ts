// src/app/features/models/items.model.ts

export const REGIONS = ['Paris','Lyon','Marseille','Bordeaux','Toulouse','Nice','Nantes','Strasbourg'] as const;
// Reflet exact de tes Enums Java
export type BodyType = 'SLIM' | 'ATHLETIC' | 'AVERAGE' | 'CURVY' | 'ROUND';
export type EyeColor = 'BROWN' | 'HAZEL' | 'GREEN' | 'BLUE' | 'GREY';
export type HairColor = 'BLACK' | 'BROWN' | 'FAIR' | 'GINGER' | 'BLOND' | 'GREY' | 'COLORED';

// Labels pour l'affichage (UI)
export const BODY_TYPE_LABELS: Record<string, string> = {
  SLIM: 'Mince', ATHLETIC: 'Athlétique', AVERAGE: 'Normale', CURVY: 'Pulpeuse', ROUND: 'Ronde'
};
// Tableau pour les itérations (Object.keys transforme l'objet en tableau de strings)
export const BODY_TYPES_LIST = Object.keys(BODY_TYPE_LABELS);

export interface ColorOption {
  label: string;
  hex: string;
}

export const EYE_COLORS: ColorOption[] = [
  { label: 'Brown',   hex: '#6b3d2e' }, { label: 'Hazel', hex: '#9b6a3a' },
  { label: 'Green',     hex: '#4a7c59' }, { label: 'Blue',     hex: '#4a7aaf' },
  { label: 'Grey',     hex: '#8a9aaa' },
];

export const HAIR_COLORS: ColorOption[] = [
  { label: 'Black',    hex: '#1a1410' }, { label: 'Brown',    hex: '#5c3d2e' },
  { label: 'Fair', hex: '#8b5e3c' }, { label: 'Blond',   hex: '#d4a847' },
  { label: 'Ginger',    hex: '#c04a1a' }, { label : 'Colored',   hex: '#8a9aaa' },
  { label: 'White',   hex: '#ffffff' }, { label : "Silver", hex: '#999999'},
];

export interface PhotoItem    { id: string; originalUrl: string; previewThumbUrl: string; }
export interface VideoItem    { id: string; url: string; duration?: string; }
export interface Review       { author: string; date: string; text: string; }
