import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class RegisterService {

  private apiUrl = 'http://localhost:8080';

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
