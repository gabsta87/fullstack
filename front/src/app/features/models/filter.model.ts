export interface GalleryFilters {
  zoneId?: number;
  bodyType?: string;
  services?: string[];
  eyeColor?: string;
  hairColor?: string;
  username?: string;
  languages?: string[];
  minAge?: string;
  maxAge?: string;
}

export interface GeographicZone {
  id: number;
  name: string;
  subZones?: GeographicZone[];
}
