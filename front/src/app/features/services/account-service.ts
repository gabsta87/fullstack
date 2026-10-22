import { Injectable } from '@angular/core';
import {firstValueFrom} from "rxjs";
import {environment} from "../../../environments/environment";
import {HttpClient} from "@angular/common/http";

@Injectable({
  providedIn: 'root',
})
export class AccountService {
  private base = `${environment.apiBase}/account`;

  constructor(private http: HttpClient) {}

  async updateSettings(data: any): Promise<any> {
    return await firstValueFrom(this.http.patch(`${this.base}/data`, data));
  }
}
