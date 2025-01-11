import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {Profile} from "../components/profile/profile.component";

@Injectable({
  providedIn: 'root'
})
export class DynamicDataService {
  private apiBaseUrl = 'http://localhost:8080/data'; // Backend URL

  constructor(private http: HttpClient) {}

  // Fetch all rows from a specific table
  getTableData(tableName: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiBaseUrl}/table/${tableName}`);
  }

  // Fetch a specific row by ID from a specific table
  getTableDataById(tableName: string, id: number): Observable<any> {
    return this.http.get<any>(`${this.apiBaseUrl}/table/${tableName}/${id}`);
  }

  getSimpleProfiles():Observable<Profile[]>{
    return this.http.get<Profile[]>(`${this.apiBaseUrl}/simpleProfiles`);
  }
}
