import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Service} from "../models/common.model";
import {environment} from "../../../environments/environment";
import {Observable} from "rxjs";
import {GeographicZone, GeographicZoneWithParent} from "../models/filter.model";

/** Current version of a public legal text (plain text, never rendered as HTML). */
export interface LegalText {
  key: string;
  language: string;
  version: number;
  content: string;
}

@Injectable({
  providedIn: 'root',
})
export class CommonService {

  constructor(private http: HttpClient) {}

  getWorkersServices() : Observable<Service[]> {
    return this.http.get<Service[]>(`${environment.apiBase}/common/services`);
  }

  getGeographicZones(): Observable<GeographicZone[]> {
    return this.http.get<GeographicZone[]>(`${environment.apiBase}/common/regions`);
  }

  /** `lang` is the site language code (fr, en, ..., ua); the backend falls back to English. */
  getLegalText(key: string, lang: string): Observable<LegalText> {
    return this.http.get<LegalText>(`${environment.apiBase}/common/legal/${key}`, { params: { lang } });
  }
}
