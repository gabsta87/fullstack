import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Service} from "../models/common.model";
import {environment} from "../../../environments/environment";
import {Observable} from "rxjs";
import {GeographicZone} from "../models/filter.model";

@Injectable({
  providedIn: 'root',
})
export class CommonService {

  constructor(private http: HttpClient) {}

  getWorkersServices() : Observable<Service[]> {
    return this.http.get<Service[]>(`${environment.apiBase}/common/services`);
  }

  getGeographicZones(): Observable<GeographicZone[]> {
    return this.http.get<GeographicZone[]>(`${environment.apiBase}/common/locations`);
  }
}
