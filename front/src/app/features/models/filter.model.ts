export interface GalleryFilters {
  region?: string;
  bodyType?: string[];
  services?: string[];
  eyeColor?: string;
  hairColor?: string;
  heightMin?: number | null;
  heightMax?: number | null;
  weightMin?: number | null;
  weightMax?: number | null;
}
