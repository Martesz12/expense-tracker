import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Account } from '../models/account/account.model';
import { AccountRequest } from '../models/account/account-request.model';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/accounts`;

  list(): Observable<Account[]> {
    return this.http.get<Account[]>(this.base);
  }

  create(body: AccountRequest): Observable<Account> {
    return this.http.post<Account>(this.base, body);
  }

  update(id: string, body: AccountRequest): Observable<Account> {
    return this.http.put<Account>(`${this.base}/${id}`, body);
  }

  archive(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
