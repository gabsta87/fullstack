import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {environment} from "../../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class ImageUploadService {
  private uploadUrl = `${environment.apiBase}/images/upload`;

  constructor(private http: HttpClient) {}

  uploadImage(file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<string>(this.uploadUrl, formData);
  }

  uploadVideo(file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post('/videos/upload', formData, { responseType: 'text' });
  }
}
