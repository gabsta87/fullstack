import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {environment} from "../../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class RegisterService {

  private apiUrl = `${environment.apiBase}`;

  constructor(private http: HttpClient) {}

  registerClient(username: string, email: string, password : string): Observable<any> {
    const body = { username, email , password};
    return this.http.post(`${this.apiUrl}/addClient`, body);
  }

  registerWorker(username: string, email: string, password : string): Observable<any> {
    const body = { username, email, password };
    return this.http.post(`${this.apiUrl}/addWorker`, body);
  }
}
