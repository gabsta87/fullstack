import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {environment} from "../../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class RegisterService {

  private apiUrl = `${environment.apiBase}/auth/register`;

  constructor(private http: HttpClient) {}

  registerClient(username: string, email: string, password : string): Observable<any> {
    const body = { username, email , password};
    return this.http.post(`${this.apiUrl}/client`, body);
  }

  registerWorker(username: string, email: string, password : string): Observable<any> {
    const body = { username, email, password };
    return this.http.post(`${this.apiUrl}/worker`, body);
  }
}
