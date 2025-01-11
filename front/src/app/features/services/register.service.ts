import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class RegisterService {

  private apiUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  registerClient(name: string, email: string): Observable<any> {
    const body = { name, email };
    return this.http.post(`${this.apiUrl}/addClient`, body);
  }

  registerWorker(name: string, email: string): Observable<any> {
    const body = { name, email };
    return this.http.post(`${this.apiUrl}/addWorker`, body);
  }
}
