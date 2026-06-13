import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AccountService } from './account.service';
import { Account } from '../models/account/account.model';
import { AccountRequest } from '../models/account/account-request.model';

const API = 'http://localhost:8080/api/accounts';

const mockAccount: Account = {
  id: '1',
  name: 'Wallet',
  type: 'CASH',
  currency: 'EUR',
  initialBalance: 0,
  balance: 50,
  color: '#ef4444',
  icon: '💰',
  archived: false,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

describe('AccountService', () => {
  let service: AccountService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AccountService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AccountService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('list() sends GET to /api/accounts', () => {
    service.list().subscribe((res) => expect(res).toEqual([mockAccount]));
    http.expectOne(API).flush([mockAccount]);
  });

  it('create() sends POST to /api/accounts with body', () => {
    const req: AccountRequest = { name: 'Wallet', type: 'CASH', currency: 'EUR' };
    service.create(req).subscribe((res) => expect(res).toEqual(mockAccount));
    const r = http.expectOne(API);
    expect(r.request.method).toBe('POST');
    expect(r.request.body).toEqual(req);
    r.flush(mockAccount);
  });

  it('update() sends PUT to /api/accounts/:id with body', () => {
    const req: AccountRequest = { name: 'Updated', type: 'CASH', currency: 'EUR' };
    service.update('1', req).subscribe((res) => expect(res).toEqual(mockAccount));
    const r = http.expectOne(`${API}/1`);
    expect(r.request.method).toBe('PUT');
    expect(r.request.body).toEqual(req);
    r.flush(mockAccount);
  });

  it('archive() sends DELETE to /api/accounts/:id', () => {
    service.archive('1').subscribe();
    const r = http.expectOne(`${API}/1`);
    expect(r.request.method).toBe('DELETE');
    r.flush(null);
  });
});
