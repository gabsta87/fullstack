import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {Observable} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private baseUrl = 'http://localhost:8080/auth';

  constructor(private http: HttpClient) {}

  login(pseudo: string, password: string): Observable<any>  {
    return this.http.post(`${this.baseUrl}/login`, { pseudo, password }, { withCredentials: true });
  }

  logout(): Observable<any>  {
    return this.http.post(`${this.baseUrl}/logout`, {}, { withCredentials: true });
  }

  isAuthenticated(): boolean {
    return document.cookie.includes('JSESSIONID');
  }
}
