import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {Observable} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private baseUrl = 'http://localhost:8080/auth';
  private redirectUrl : string = "";

  constructor(private http: HttpClient) {}

  login(pseudo: string, password: string, redirectUrl: string): Observable<any>  {
    return this.http.post(`${this.baseUrl}/login`, { pseudo, password, redirectUrl}, { withCredentials: true });
  }

  logout(): Observable<any>  {
    return this.http.post(`${this.baseUrl}/logout`, {}, { withCredentials: true });
  }

  checkSession(): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/session-check`, { withCredentials: true });
  }

  setRedirectUrl(url: string) {
    this.redirectUrl = url;
  }

  getRedirectUrl() {
    return this.redirectUrl;
  }
}
