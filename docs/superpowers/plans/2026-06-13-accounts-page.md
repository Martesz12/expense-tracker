# Accounts Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the full `/accounts` page — master-detail layout, NgRx accounts slice, HTTP services, add/edit modal, archive/delete actions — as specified in `docs/superpowers/specs/2026-06-13-accounts-page-design.md`.

**Architecture:** Smart/dumb component split with Angular signals throughout. `AccountsComponent` (smart page shell) converts NgRx selectors to signals via `toSignal()` and passes signal values to `AccountListComponent` (left panel) and `AccountDetailComponent` (right panel) which use the new `input()`/`output()` signal API. `AccountModalComponent` is semi-smart: it uses `effect()` to react to the `visible` input signal, `toSignal()` for store state, and `output()` for the close event. No `AsyncPipe` or `Observable` subscriptions in component classes or templates — signals all the way down.

**Tech Stack:** Angular 21, NgRx 18, Angular signals (`toSignal`, `input`, `output`, `effect`, `untracked` from `@angular/core` / `@angular/core/rxjs-interop`), PrimeNG (Dialog, Menu, Select, InputNumber, ConfirmDialog), Vitest, SCSS tokens from `frontend/src/styles/_variables.scss`.

> **Before every commit:** Run `/change-review`, fix every issue it flags, then proceed to the commit step.

> **Known backend limitation:** `GET /api/accounts` returns only non-archived accounts. The spec requires archived accounts to remain visible (dimmed). Workaround: on archive success, update the account in local store to `{ archived: true }` so it stays dimmed until page refresh.
>
> **Note:** "Delete" in the UI maps to the archive endpoint (`DELETE /api/accounts/{id}`) on the backend — no hard-delete endpoint exists. Delete differs from Archive only in that delete removes the account from the in-store list entirely (no dimming) and requires a confirm dialog first.

---

## File Structure

| File | Role |
|------|------|
| `core/models/account/account.model.ts` | Account interface + AccountType |
| `core/models/account/account-request.model.ts` | AccountRequest DTO |
| `core/models/transaction/transaction-response.model.ts` | TransactionResponse (minimal) |
| `core/models/common/page-response.model.ts` | Generic Spring Page shape |
| `core/services/account.service.ts` | HTTP: list, create, update, archive |
| `core/services/transaction.service.ts` | HTTP: list with filters |
| `features/accounts/pipes/account-type.pipe.ts` | `CREDIT_CARD` → `Credit Card` |
| `features/accounts/store/accounts.state.ts` | NgRx state interface + initial state |
| `features/accounts/store/accounts.actions.ts` | All accounts actions |
| `features/accounts/store/accounts.reducer.ts` | createFeature reducer + exported selectors |
| `features/accounts/store/accounts.effects.ts` | HTTP effects |
| `features/accounts/store/accounts.selectors.ts` | `selectSelectedAccount` derived selector |
| `features/accounts/accounts.component.{ts,html,scss}` | Smart page shell — `toSignal()` for all store state |
| `features/accounts/account-list/account-list.component.{ts,html,scss}` | Dumb left-panel list — `input()`/`output()` |
| `features/accounts/account-detail/account-detail.component.{ts,html,scss}` | Dumb right-panel detail — `input()`/`output()` |
| `features/accounts/account-modal/account-modal.component.{ts,html,scss}` | Semi-smart add/edit dialog — `input()`/`output()` + `effect()` |
| **Modified:** `app/app.routes.ts` | Add `accounts` child route |
| **Modified:** `app/app.config.ts` | Register `accountsReducer` + `AccountsEffects` |

All paths are relative to `frontend/src/app/`.

---

## Task 1: Domain Models

**Files:**
- Create: `frontend/src/app/core/models/account/account.model.ts`
- Create: `frontend/src/app/core/models/account/account-request.model.ts`
- Create: `frontend/src/app/core/models/transaction/transaction-response.model.ts`
- Create: `frontend/src/app/core/models/common/page-response.model.ts`

No tests needed — plain interfaces.

- [ ] **Step 1: Create account.model.ts**

```typescript
export type AccountType = 'CASH' | 'BANK' | 'CREDIT_CARD' | 'SAVINGS' | 'INVESTMENT' | 'OTHER';

export interface Account {
  id: string;
  name: string;
  type: AccountType;
  currency: string;
  initialBalance: number;
  balance: number;
  color: string | null;
  icon: string | null;
  archived: boolean;
  createdAt: string;
  updatedAt: string;
}
```

- [ ] **Step 2: Create account-request.model.ts**

```typescript
export interface AccountRequest {
  name: string;
  type: string;
  currency: string;
  initialBalance?: number | null;
  color?: string | null;
  icon?: string | null;
}
```

- [ ] **Step 3: Create transaction-response.model.ts**

```typescript
export interface TransactionResponse {
  id: string;
  type: 'INCOME' | 'EXPENSE' | 'TRANSFER';
  amount: number;
  currency: string;
  fromAccountId: string;
  toAccountId: string | null;
  categoryId: string | null;
  categoryName: string | null;
  note: string | null;
  transactionDate: string;
}
```

- [ ] **Step 4: Create page-response.model.ts**

```typescript
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  last: boolean;
}
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/core/models/account/ frontend/src/app/core/models/transaction/ frontend/src/app/core/models/common/
git commit -m "feat: add account, transaction, and page-response models"
```

---

## Task 2: AccountTypePipe

**Files:**
- Create: `frontend/src/app/features/accounts/pipes/account-type.pipe.ts`
- Test: `frontend/src/app/features/accounts/pipes/account-type.pipe.spec.ts`

- [ ] **Step 1: Write the failing test**

```typescript
// account-type.pipe.spec.ts
import { AccountTypePipe } from './account-type.pipe';

describe('AccountTypePipe', () => {
  const pipe = new AccountTypePipe();

  it('transforms CASH to Cash', () => expect(pipe.transform('CASH')).toBe('Cash'));
  it('transforms CREDIT_CARD to Credit Card', () => expect(pipe.transform('CREDIT_CARD')).toBe('Credit Card'));
  it('transforms unknown value to the original string', () => expect(pipe.transform('FOO')).toBe('FOO'));
});
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd frontend && ng test --watch=false 2>&1 | grep -A3 "AccountTypePipe"
```

Expected: FAIL — `AccountTypePipe` not defined.

- [ ] **Step 3: Create account-type.pipe.ts**

```typescript
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'accountType', standalone: true })
export class AccountTypePipe implements PipeTransform {
  private readonly labels: Record<string, string> = {
    CASH: 'Cash',
    BANK: 'Bank',
    CREDIT_CARD: 'Credit Card',
    SAVINGS: 'Savings',
    INVESTMENT: 'Investment',
    OTHER: 'Other',
  };

  transform(type: string): string {
    return this.labels[type] ?? type;
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd frontend && ng test --watch=false 2>&1 | grep -A3 "AccountTypePipe"
```

Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/features/accounts/pipes/
git commit -m "feat: add AccountTypePipe"
```

---

## Task 3: AccountService + Tests

**Files:**
- Create: `frontend/src/app/core/services/account.service.ts`
- Test: `frontend/src/app/core/services/account.service.spec.ts`

- [ ] **Step 1: Write the failing tests**

```typescript
// account.service.spec.ts
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AccountService } from './account.service';
import { Account } from '../models/account/account.model';
import { AccountRequest } from '../models/account/account-request.model';

const API = 'http://localhost:8080/api/accounts';

const mockAccount: Account = {
  id: '1', name: 'Wallet', type: 'CASH', currency: 'EUR',
  initialBalance: 0, balance: 50, color: '#ef4444', icon: '💰',
  archived: false, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
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
    service.list().subscribe(res => expect(res).toEqual([mockAccount]));
    http.expectOne(API).flush([mockAccount]);
  });

  it('create() sends POST to /api/accounts with body', () => {
    const req: AccountRequest = { name: 'Wallet', type: 'CASH', currency: 'EUR' };
    service.create(req).subscribe(res => expect(res).toEqual(mockAccount));
    const r = http.expectOne(API);
    expect(r.request.method).toBe('POST');
    expect(r.request.body).toEqual(req);
    r.flush(mockAccount);
  });

  it('update() sends PUT to /api/accounts/:id with body', () => {
    const req: AccountRequest = { name: 'Updated', type: 'CASH', currency: 'EUR' };
    service.update('1', req).subscribe(res => expect(res).toEqual(mockAccount));
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
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd frontend && ng test --watch=false 2>&1 | grep -A3 "AccountService"
```

Expected: FAIL — `AccountService` not found.

- [ ] **Step 3: Create account.service.ts**

```typescript
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
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd frontend && ng test --watch=false 2>&1 | grep -A3 "AccountService"
```

Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/core/services/account.service.ts frontend/src/app/core/services/account.service.spec.ts
git commit -m "feat: add AccountService"
```

---

## Task 4: TransactionService + Tests

**Files:**
- Create: `frontend/src/app/core/services/transaction.service.ts`
- Test: `frontend/src/app/core/services/transaction.service.spec.ts`

- [ ] **Step 1: Write the failing tests**

```typescript
// transaction.service.spec.ts
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { TransactionService } from './transaction.service';
import { TransactionResponse } from '../models/transaction/transaction-response.model';
import { PageResponse } from '../models/common/page-response.model';

const API = 'http://localhost:8080/api/transactions';

const mockTx: TransactionResponse = {
  id: '1', type: 'EXPENSE', amount: 20, currency: 'EUR',
  fromAccountId: 'acc1', toAccountId: null, categoryId: null,
  categoryName: 'Food', note: null, transactionDate: '2026-06-01T10:00:00Z',
};

const mockPage: PageResponse<TransactionResponse> = {
  content: [mockTx], totalElements: 1, totalPages: 1, last: true,
};

describe('TransactionService', () => {
  let service: TransactionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [TransactionService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TransactionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('list() passes accountId and size as query params', () => {
    service.list({ accountId: 'acc1', page: 0, size: 5 }).subscribe(res => expect(res).toEqual(mockPage));
    const req = http.expectOne(r => r.url === API);
    expect(req.request.params.get('accountId')).toBe('acc1');
    expect(req.request.params.get('size')).toBe('5');
    expect(req.request.params.get('page')).toBe('0');
    req.flush(mockPage);
  });

  it('list() omits undefined params', () => {
    service.list({}).subscribe();
    const req = http.expectOne(r => r.url === API);
    expect(req.request.params.keys().length).toBe(0);
    req.flush(mockPage);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd frontend && ng test --watch=false 2>&1 | grep -A3 "TransactionService"
```

Expected: FAIL.

- [ ] **Step 3: Create transaction.service.ts**

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { TransactionResponse } from '../models/transaction/transaction-response.model';
import { PageResponse } from '../models/common/page-response.model';

export interface TransactionListParams {
  accountId?: string;
  page?: number;
  size?: number;
  from?: string;
  to?: string;
  type?: string;
}

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/transactions`;

  list(params: TransactionListParams): Observable<PageResponse<TransactionResponse>> {
    let p = new HttpParams();
    if (params.accountId != null) p = p.set('accountId', params.accountId);
    if (params.page != null) p = p.set('page', params.page);
    if (params.size != null) p = p.set('size', params.size);
    if (params.from != null) p = p.set('from', params.from);
    if (params.to != null) p = p.set('to', params.to);
    if (params.type != null) p = p.set('type', params.type);
    return this.http.get<PageResponse<TransactionResponse>>(this.base, { params: p });
  }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd frontend && ng test --watch=false 2>&1 | grep -A3 "TransactionService"
```

Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/core/services/transaction.service.ts frontend/src/app/core/services/transaction.service.spec.ts
git commit -m "feat: add TransactionService"
```

---

## Task 5: NgRx State & Actions

**Files:**
- Create: `frontend/src/app/features/accounts/store/accounts.state.ts`
- Create: `frontend/src/app/features/accounts/store/accounts.actions.ts`

No tests at this step — state/actions are covered by reducer + effects tests.

- [ ] **Step 1: Create accounts.state.ts**

```typescript
import { Account } from '../../../core/models/account/account.model';
import { TransactionResponse } from '../../../core/models/transaction/transaction-response.model';

export interface AccountsState {
  accounts: Account[];
  selectedAccountId: string | null;
  recentTransactions: TransactionResponse[];
  monthlyIncome: number | null;
  monthlyExpense: number | null;
  loadingAccounts: boolean;
  loadingDetail: boolean;
  error: string | null;
  modalOpen: boolean;
  editingAccount: Account | null;
  saving: boolean;
  saveError: string | null;
}

export const initialAccountsState: AccountsState = {
  accounts: [],
  selectedAccountId: null,
  recentTransactions: [],
  monthlyIncome: null,
  monthlyExpense: null,
  loadingAccounts: false,
  loadingDetail: false,
  error: null,
  modalOpen: false,
  editingAccount: null,
  saving: false,
  saveError: null,
};
```

- [ ] **Step 2: Create accounts.actions.ts**

```typescript
import { createActionGroup, emptyProps, props } from '@ngrx/store';

import { Account } from '../../../core/models/account/account.model';
import { AccountRequest } from '../../../core/models/account/account-request.model';
import { TransactionResponse } from '../../../core/models/transaction/transaction-response.model';

export const AccountsActions = createActionGroup({
  source: 'Accounts',
  events: {
    'Load Accounts': emptyProps(),
    'Load Accounts Success': props<{ accounts: Account[] }>(),
    'Load Accounts Failure': props<{ error: string }>(),

    'Select Account': props<{ accountId: string }>(),
    'Load Account Detail Success': props<{
      accountId: string;
      transactions: TransactionResponse[];
      monthlyIncome: number;
      monthlyExpense: number;
    }>(),
    'Load Account Detail Failure': props<{ error: string }>(),

    'Open Add Modal': emptyProps(),
    'Open Edit Modal': props<{ account: Account }>(),
    'Close Modal': emptyProps(),

    'Save Account': props<{ request: AccountRequest; accountId?: string }>(),
    'Save Account Success': props<{ account: Account; isUpdate: boolean }>(),
    'Save Account Failure': props<{ error: string }>(),

    'Archive Account': props<{ accountId: string }>(),
    'Archive Account Success': props<{ accountId: string }>(),
    'Archive Account Failure': props<{ error: string }>(),

    'Delete Account': props<{ accountId: string }>(),
    'Delete Account Success': props<{ accountId: string }>(),
    'Delete Account Failure': props<{ error: string }>(),
  },
});
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/features/accounts/store/accounts.state.ts frontend/src/app/features/accounts/store/accounts.actions.ts
git commit -m "feat: add accounts NgRx state and actions"
```

---

## Task 6: NgRx Reducer + Reducer Tests

**Files:**
- Create: `frontend/src/app/features/accounts/store/accounts.reducer.ts`
- Test: `frontend/src/app/features/accounts/store/accounts.reducer.spec.ts`

- [ ] **Step 1: Write the failing tests**

```typescript
// accounts.reducer.spec.ts
import { Account } from '../../../core/models/account/account.model';
import { AccountsActions } from './accounts.actions';
import { accountsReducer } from './accounts.reducer';
import { initialAccountsState } from './accounts.state';

const mockAccount: Account = {
  id: '1', name: 'Wallet', type: 'CASH', currency: 'EUR',
  initialBalance: 0, balance: 50, color: null, icon: null,
  archived: false, createdAt: '', updatedAt: '',
};

describe('accountsReducer', () => {
  it('returns initial state for unknown actions', () => {
    const state = accountsReducer(undefined, { type: '@@INIT' } as never);
    expect(state).toEqual(initialAccountsState);
  });

  it('loadAccountsSuccess: sets accounts, clears loadingAccounts', () => {
    const state = accountsReducer(
      { ...initialAccountsState, loadingAccounts: true },
      AccountsActions.loadAccountsSuccess({ accounts: [mockAccount] }),
    );
    expect(state.accounts).toEqual([mockAccount]);
    expect(state.loadingAccounts).toBe(false);
  });

  it('selectAccount: sets selectedAccountId, loadingDetail=true, clears detail', () => {
    const state = accountsReducer(
      initialAccountsState,
      AccountsActions.selectAccount({ accountId: '1' }),
    );
    expect(state.selectedAccountId).toBe('1');
    expect(state.loadingDetail).toBe(true);
    expect(state.recentTransactions).toEqual([]);
    expect(state.monthlyIncome).toBeNull();
  });

  it('saveAccountSuccess (create): appends account to list, closes modal', () => {
    const state = accountsReducer(
      { ...initialAccountsState, accounts: [mockAccount], saving: true, modalOpen: true },
      AccountsActions.saveAccountSuccess({ account: { ...mockAccount, id: '2' }, isUpdate: false }),
    );
    expect(state.accounts).toHaveLength(2);
    expect(state.modalOpen).toBe(false);
    expect(state.saving).toBe(false);
  });

  it('saveAccountSuccess (update): replaces matching account, closes modal', () => {
    const updated = { ...mockAccount, name: 'Updated Wallet' };
    const state = accountsReducer(
      { ...initialAccountsState, accounts: [mockAccount], saving: true, modalOpen: true },
      AccountsActions.saveAccountSuccess({ account: updated, isUpdate: true }),
    );
    expect(state.accounts[0].name).toBe('Updated Wallet');
    expect(state.accounts).toHaveLength(1);
    expect(state.modalOpen).toBe(false);
  });

  it('archiveAccountSuccess: marks account archived, clears selectedAccountId', () => {
    const state = accountsReducer(
      { ...initialAccountsState, accounts: [mockAccount], selectedAccountId: '1' },
      AccountsActions.archiveAccountSuccess({ accountId: '1' }),
    );
    expect(state.accounts[0].archived).toBe(true);
    expect(state.selectedAccountId).toBeNull();
  });

  it('deleteAccountSuccess: removes account from list, clears selectedAccountId', () => {
    const state = accountsReducer(
      { ...initialAccountsState, accounts: [mockAccount], selectedAccountId: '1' },
      AccountsActions.deleteAccountSuccess({ accountId: '1' }),
    );
    expect(state.accounts).toHaveLength(0);
    expect(state.selectedAccountId).toBeNull();
  });

  it('openAddModal: sets modalOpen=true, editingAccount=null', () => {
    const state = accountsReducer(initialAccountsState, AccountsActions.openAddModal());
    expect(state.modalOpen).toBe(true);
    expect(state.editingAccount).toBeNull();
  });

  it('openEditModal: sets modalOpen=true, editingAccount=account', () => {
    const state = accountsReducer(
      initialAccountsState,
      AccountsActions.openEditModal({ account: mockAccount }),
    );
    expect(state.modalOpen).toBe(true);
    expect(state.editingAccount).toEqual(mockAccount);
  });

  it('closeModal: sets modalOpen=false, editingAccount=null', () => {
    const state = accountsReducer(
      { ...initialAccountsState, modalOpen: true, editingAccount: mockAccount },
      AccountsActions.closeModal(),
    );
    expect(state.modalOpen).toBe(false);
    expect(state.editingAccount).toBeNull();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd frontend && ng test --watch=false 2>&1 | grep -A3 "accountsReducer"
```

Expected: FAIL — `accountsReducer` not found.

- [ ] **Step 3: Create accounts.reducer.ts**

```typescript
import { createFeature, createReducer, on } from '@ngrx/store';

import { AccountsActions } from './accounts.actions';
import { AccountsState, initialAccountsState } from './accounts.state';

export const accountsFeature = createFeature({
  name: 'accounts',
  reducer: createReducer(
    initialAccountsState,

    on(AccountsActions.loadAccounts, (state): AccountsState => ({
      ...state, loadingAccounts: true, error: null,
    })),

    on(AccountsActions.loadAccountsSuccess, (state, { accounts }): AccountsState => ({
      ...state, loadingAccounts: false, accounts,
    })),

    on(AccountsActions.loadAccountsFailure, (state, { error }): AccountsState => ({
      ...state, loadingAccounts: false, error,
    })),

    on(AccountsActions.selectAccount, (state, { accountId }): AccountsState => ({
      ...state,
      selectedAccountId: accountId,
      recentTransactions: [],
      monthlyIncome: null,
      monthlyExpense: null,
      loadingDetail: true,
    })),

    on(AccountsActions.loadAccountDetailSuccess, (state, { transactions, monthlyIncome, monthlyExpense }): AccountsState => ({
      ...state, loadingDetail: false, recentTransactions: transactions, monthlyIncome, monthlyExpense,
    })),

    on(AccountsActions.loadAccountDetailFailure, (state, { error }): AccountsState => ({
      ...state, loadingDetail: false, error,
    })),

    on(AccountsActions.openAddModal, (state): AccountsState => ({
      ...state, modalOpen: true, editingAccount: null, saveError: null,
    })),

    on(AccountsActions.openEditModal, (state, { account }): AccountsState => ({
      ...state, modalOpen: true, editingAccount: account, saveError: null,
    })),

    on(AccountsActions.closeModal, (state): AccountsState => ({
      ...state, modalOpen: false, editingAccount: null, saveError: null,
    })),

    on(AccountsActions.saveAccount, (state): AccountsState => ({
      ...state, saving: true, saveError: null,
    })),

    on(AccountsActions.saveAccountSuccess, (state, { account, isUpdate }): AccountsState => ({
      ...state,
      saving: false,
      modalOpen: false,
      editingAccount: null,
      accounts: isUpdate
        ? state.accounts.map(a => (a.id === account.id ? account : a))
        : [...state.accounts, account],
    })),

    on(AccountsActions.saveAccountFailure, (state, { error }): AccountsState => ({
      ...state, saving: false, saveError: error,
    })),

    on(AccountsActions.archiveAccountSuccess, (state, { accountId }): AccountsState => ({
      ...state,
      accounts: state.accounts.map(a => (a.id === accountId ? { ...a, archived: true } : a)),
      selectedAccountId: state.selectedAccountId === accountId ? null : state.selectedAccountId,
    })),

    on(AccountsActions.archiveAccountFailure, AccountsActions.deleteAccountFailure, (state, { error }): AccountsState => ({
      ...state, error,
    })),

    on(AccountsActions.deleteAccountSuccess, (state, { accountId }): AccountsState => ({
      ...state,
      accounts: state.accounts.filter(a => a.id !== accountId),
      selectedAccountId: state.selectedAccountId === accountId ? null : state.selectedAccountId,
    })),
  ),
});

export const {
  name: accountsFeatureKey,
  reducer: accountsReducer,
  selectAccounts,
  selectSelectedAccountId,
  selectRecentTransactions,
  selectMonthlyIncome,
  selectMonthlyExpense,
  selectLoadingAccounts,
  selectLoadingDetail,
  selectError,
  selectModalOpen,
  selectEditingAccount,
  selectSaving,
  selectSaveError,
} = accountsFeature;
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd frontend && ng test --watch=false 2>&1 | grep -A3 "accountsReducer"
```

Expected: PASS (9 tests).

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/features/accounts/store/accounts.reducer.ts frontend/src/app/features/accounts/store/accounts.reducer.spec.ts
git commit -m "feat: add accounts NgRx reducer"
```

---

## Task 7: NgRx Effects + Effects Tests

**Files:**
- Create: `frontend/src/app/features/accounts/store/accounts.effects.ts`
- Test: `frontend/src/app/features/accounts/store/accounts.effects.spec.ts`

- [ ] **Step 1: Write the failing tests**

```typescript
// accounts.effects.spec.ts
import { TestBed } from '@angular/core/testing';
import { provideMockActions } from '@ngrx/effects/testing';
import { Observable, firstValueFrom, of, throwError } from 'rxjs';

import { AccountsEffects } from './accounts.effects';
import { AccountsActions } from './accounts.actions';
import { AccountService } from '../../../core/services/account.service';
import { TransactionService } from '../../../core/services/transaction.service';
import { Account } from '../../../core/models/account/account.model';
import { TransactionResponse } from '../../../core/models/transaction/transaction-response.model';
import { PageResponse } from '../../../core/models/common/page-response.model';

const mockAccount: Account = {
  id: '1', name: 'Wallet', type: 'CASH', currency: 'EUR',
  initialBalance: 0, balance: 50, color: null, icon: null,
  archived: false, createdAt: '', updatedAt: '',
};

const incomeTx: TransactionResponse = {
  id: 't1', type: 'INCOME', amount: 100, currency: 'EUR',
  fromAccountId: '1', toAccountId: null, categoryId: null,
  categoryName: null, note: null, transactionDate: '2026-06-01T00:00:00Z',
};

const expenseTx: TransactionResponse = {
  id: 't2', type: 'EXPENSE', amount: 30, currency: 'EUR',
  fromAccountId: '1', toAccountId: null, categoryId: null,
  categoryName: null, note: null, transactionDate: '2026-06-05T00:00:00Z',
};

const recentPage: PageResponse<TransactionResponse> = {
  content: [expenseTx], totalElements: 1, totalPages: 1, last: true,
};

const monthlyPage: PageResponse<TransactionResponse> = {
  content: [incomeTx, expenseTx], totalElements: 2, totalPages: 1, last: true,
};

describe('AccountsEffects', () => {
  let actions$: Observable<unknown>;
  let effects: AccountsEffects;
  let mockAccountService: {
    list: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    archive: ReturnType<typeof vi.fn>;
  };
  let mockTransactionService: { list: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    mockAccountService = { list: vi.fn(), create: vi.fn(), update: vi.fn(), archive: vi.fn() };
    mockTransactionService = { list: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        AccountsEffects,
        provideMockActions(() => actions$),
        { provide: AccountService, useValue: mockAccountService },
        { provide: TransactionService, useValue: mockTransactionService },
      ],
    });

    effects = TestBed.inject(AccountsEffects);
  });

  describe('loadAccounts$', () => {
    it('dispatches loadAccountsSuccess on service success', async () => {
      mockAccountService.list.mockReturnValue(of([mockAccount]));
      actions$ = of(AccountsActions.loadAccounts());

      const result = await firstValueFrom(effects.loadAccounts$);

      expect(result).toEqual(AccountsActions.loadAccountsSuccess({ accounts: [mockAccount] }));
    });

    it('dispatches loadAccountsFailure on service error', async () => {
      mockAccountService.list.mockReturnValue(throwError(() => new Error('Network error')));
      actions$ = of(AccountsActions.loadAccounts());

      const result = await firstValueFrom(effects.loadAccounts$);

      expect(result).toEqual(AccountsActions.loadAccountsFailure({
        error: 'An unexpected error occurred. Please try again.',
      }));
    });
  });

  describe('selectAccount$', () => {
    it('dispatches loadAccountDetailSuccess with computed income and expense', async () => {
      mockTransactionService.list
        .mockReturnValueOnce(of(recentPage))
        .mockReturnValueOnce(of(monthlyPage));
      actions$ = of(AccountsActions.selectAccount({ accountId: '1' }));

      const result = await firstValueFrom(effects.selectAccount$);

      expect(result).toEqual(AccountsActions.loadAccountDetailSuccess({
        accountId: '1',
        transactions: [expenseTx],
        monthlyIncome: 100,
        monthlyExpense: 30,
      }));
    });

    it('dispatches loadAccountDetailFailure on API error', async () => {
      mockTransactionService.list.mockReturnValue(throwError(() => new Error('err')));
      actions$ = of(AccountsActions.selectAccount({ accountId: '1' }));

      const result = await firstValueFrom(effects.selectAccount$);

      expect(result).toEqual(AccountsActions.loadAccountDetailFailure({
        error: 'An unexpected error occurred. Please try again.',
      }));
    });
  });

  describe('saveAccount$', () => {
    it('calls create() and dispatches saveAccountSuccess with isUpdate=false when no accountId', async () => {
      mockAccountService.create.mockReturnValue(of(mockAccount));
      actions$ = of(AccountsActions.saveAccount({
        request: { name: 'Wallet', type: 'CASH', currency: 'EUR' },
      }));

      const result = await firstValueFrom(effects.saveAccount$);

      expect(mockAccountService.create).toHaveBeenCalled();
      expect(result).toEqual(AccountsActions.saveAccountSuccess({ account: mockAccount, isUpdate: false }));
    });

    it('calls update() and dispatches saveAccountSuccess with isUpdate=true when accountId given', async () => {
      mockAccountService.update.mockReturnValue(of(mockAccount));
      actions$ = of(AccountsActions.saveAccount({
        request: { name: 'Wallet', type: 'CASH', currency: 'EUR' },
        accountId: '1',
      }));

      const result = await firstValueFrom(effects.saveAccount$);

      expect(mockAccountService.update).toHaveBeenCalledWith('1', expect.any(Object));
      expect(result).toEqual(AccountsActions.saveAccountSuccess({ account: mockAccount, isUpdate: true }));
    });

    it('dispatches saveAccountFailure on error', async () => {
      mockAccountService.create.mockReturnValue(throwError(() => new Error('fail')));
      actions$ = of(AccountsActions.saveAccount({ request: { name: 'W', type: 'CASH', currency: 'EUR' } }));

      const result = await firstValueFrom(effects.saveAccount$);

      expect(result).toEqual(AccountsActions.saveAccountFailure({
        error: 'An unexpected error occurred. Please try again.',
      }));
    });
  });

  describe('archiveAccount$', () => {
    it('calls archive() and dispatches archiveAccountSuccess', async () => {
      mockAccountService.archive.mockReturnValue(of(void 0));
      actions$ = of(AccountsActions.archiveAccount({ accountId: '1' }));

      const result = await firstValueFrom(effects.archiveAccount$);

      expect(result).toEqual(AccountsActions.archiveAccountSuccess({ accountId: '1' }));
    });
  });

  describe('deleteAccount$', () => {
    it('calls archive() and dispatches deleteAccountSuccess', async () => {
      mockAccountService.archive.mockReturnValue(of(void 0));
      actions$ = of(AccountsActions.deleteAccount({ accountId: '1' }));

      const result = await firstValueFrom(effects.deleteAccount$);

      expect(result).toEqual(AccountsActions.deleteAccountSuccess({ accountId: '1' }));
    });
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd frontend && ng test --watch=false 2>&1 | grep -A5 "AccountsEffects"
```

Expected: FAIL — `AccountsEffects` not found.

- [ ] **Step 3: Create accounts.effects.ts**

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, exhaustMap, forkJoin, map, of, switchMap } from 'rxjs';

import { AccountsActions } from './accounts.actions';
import { AccountService } from '../../../core/services/account.service';
import { TransactionService } from '../../../core/services/transaction.service';
import { ApiError } from '../../../core/models/auth/api-error.model';

function extractErrorMessage(err: unknown): string {
  const body = (err as HttpErrorResponse)?.error as ApiError | null;
  return body?.message ?? 'An unexpected error occurred. Please try again.';
}

@Injectable()
export class AccountsEffects {
  private readonly actions$ = inject(Actions);
  private readonly accountService = inject(AccountService);
  private readonly transactionService = inject(TransactionService);

  loadAccounts$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AccountsActions.loadAccounts),
      switchMap(() =>
        this.accountService.list().pipe(
          map(accounts => AccountsActions.loadAccountsSuccess({ accounts })),
          catchError((err: unknown) =>
            of(AccountsActions.loadAccountsFailure({ error: extractErrorMessage(err) })),
          ),
        ),
      ),
    ),
  );

  selectAccount$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AccountsActions.selectAccount),
      switchMap(({ accountId }) => {
        const now = new Date();
        const from = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`;
        const to = now.toISOString().slice(0, 10);
        return forkJoin({
          recent: this.transactionService.list({ accountId, page: 0, size: 5 }),
          monthly: this.transactionService.list({ accountId, from, to, size: 500 }),
        }).pipe(
          map(({ recent, monthly }) => {
            const monthlyIncome = monthly.content
              .filter(t => t.type === 'INCOME')
              .reduce((sum, t) => sum + t.amount, 0);
            const monthlyExpense = monthly.content
              .filter(t => t.type === 'EXPENSE')
              .reduce((sum, t) => sum + t.amount, 0);
            return AccountsActions.loadAccountDetailSuccess({
              accountId,
              transactions: recent.content,
              monthlyIncome,
              monthlyExpense,
            });
          }),
          catchError((err: unknown) =>
            of(AccountsActions.loadAccountDetailFailure({ error: extractErrorMessage(err) })),
          ),
        );
      }),
    ),
  );

  saveAccount$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AccountsActions.saveAccount),
      exhaustMap(({ request, accountId }) => {
        const call = accountId
          ? this.accountService.update(accountId, request)
          : this.accountService.create(request);
        return call.pipe(
          map(account => AccountsActions.saveAccountSuccess({ account, isUpdate: !!accountId })),
          catchError((err: unknown) =>
            of(AccountsActions.saveAccountFailure({ error: extractErrorMessage(err) })),
          ),
        );
      }),
    ),
  );

  archiveAccount$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AccountsActions.archiveAccount),
      exhaustMap(({ accountId }) =>
        this.accountService.archive(accountId).pipe(
          map(() => AccountsActions.archiveAccountSuccess({ accountId })),
          catchError((err: unknown) =>
            of(AccountsActions.archiveAccountFailure({ error: extractErrorMessage(err) })),
          ),
        ),
      ),
    ),
  );

  deleteAccount$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AccountsActions.deleteAccount),
      exhaustMap(({ accountId }) =>
        this.accountService.archive(accountId).pipe(
          map(() => AccountsActions.deleteAccountSuccess({ accountId })),
          catchError((err: unknown) =>
            of(AccountsActions.deleteAccountFailure({ error: extractErrorMessage(err) })),
          ),
        ),
      ),
    ),
  );
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd frontend && ng test --watch=false 2>&1 | grep -A5 "AccountsEffects"
```

Expected: PASS (7 tests).

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/features/accounts/store/accounts.effects.ts frontend/src/app/features/accounts/store/accounts.effects.spec.ts
git commit -m "feat: add accounts NgRx effects"
```

---

## Task 8: NgRx Selectors

**Files:**
- Create: `frontend/src/app/features/accounts/store/accounts.selectors.ts`

- [ ] **Step 1: Create accounts.selectors.ts**

```typescript
import { createSelector } from '@ngrx/store';

import { selectAccounts, selectSelectedAccountId } from './accounts.reducer';

export const selectSelectedAccount = createSelector(
  selectAccounts,
  selectSelectedAccountId,
  (accounts, selectedId) => (selectedId ? (accounts.find(a => a.id === selectedId) ?? null) : null),
);
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/app/features/accounts/store/accounts.selectors.ts
git commit -m "feat: add selectSelectedAccount derived selector"
```

---

## Task 9: Wire Route + App Config

**Files:**
- Modify: `frontend/src/app/app.routes.ts`
- Modify: `frontend/src/app/app.config.ts`

- [ ] **Step 1: Add accounts route to app.routes.ts**

Add the accounts child route inside the shell's `children` array, before the redirect:

```typescript
// The shell children should be:
children: [
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
  },
  {
    path: 'accounts',
    loadComponent: () =>
      import('./features/accounts/accounts.component').then((m) => m.AccountsComponent),
  },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
],
```

- [ ] **Step 2: Register accounts NgRx slice in app.config.ts**

Add imports at the top:
```typescript
import { accountsFeatureKey, accountsReducer } from './features/accounts/store/accounts.reducer';
import { AccountsEffects } from './features/accounts/store/accounts.effects';
```

Update `provideStore` and `provideEffects`:
```typescript
provideStore({ [authFeatureKey]: authReducer, [accountsFeatureKey]: accountsReducer }),
provideEffects([AuthEffects, AccountsEffects]),
```

- [ ] **Step 3: Verify the app compiles**

```bash
cd frontend && ng build 2>&1 | tail -5
```

Expected: Build succeeds with no errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/app.routes.ts frontend/src/app/app.config.ts
git commit -m "feat: register accounts route and NgRx slice in app config"
```

---

## Task 10: AccountsComponent (Page Shell)

**Signal strategy:** `toSignal()` converts every NgRx selector to a signal. No `AsyncPipe`, no `$` suffix, no `| async` in the template — just call `accounts()`, `selectedAccountId()`, etc.

**Files:**
- Create: `frontend/src/app/features/accounts/accounts.component.ts`
- Create: `frontend/src/app/features/accounts/accounts.component.html`
- Create: `frontend/src/app/features/accounts/accounts.component.scss`

- [ ] **Step 1: Create accounts.component.ts**

```typescript
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Store } from '@ngrx/store';
import { ConfirmationService } from 'primeng/api';
import { ConfirmDialog } from 'primeng/confirmdialog';

import { AccountsActions } from './store/accounts.actions';
import { Account } from '../../core/models/account/account.model';
import {
  selectAccounts,
  selectEditingAccount,
  selectLoadingAccounts,
  selectLoadingDetail,
  selectModalOpen,
  selectMonthlyExpense,
  selectMonthlyIncome,
  selectRecentTransactions,
  selectSelectedAccountId,
} from './store/accounts.reducer';
import { selectSelectedAccount } from './store/accounts.selectors';
import { AccountListComponent } from './account-list/account-list.component';
import { AccountDetailComponent } from './account-detail/account-detail.component';
import { AccountModalComponent } from './account-modal/account-modal.component';

@Component({
  selector: 'app-accounts',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [ConfirmationService],
  imports: [AccountListComponent, AccountDetailComponent, AccountModalComponent, ConfirmDialog],
  templateUrl: './accounts.component.html',
  styleUrl: './accounts.component.scss',
})
export class AccountsComponent implements OnInit {
  private readonly store = inject(Store);
  private readonly confirmService = inject(ConfirmationService);

  readonly accounts = toSignal(this.store.select(selectAccounts), { initialValue: [] as Account[] });
  readonly selectedAccountId = toSignal(this.store.select(selectSelectedAccountId), { initialValue: null });
  readonly selectedAccount = toSignal(this.store.select(selectSelectedAccount), { initialValue: null });
  readonly recentTransactions = toSignal(this.store.select(selectRecentTransactions), { initialValue: [] });
  readonly monthlyIncome = toSignal(this.store.select(selectMonthlyIncome), { initialValue: null });
  readonly monthlyExpense = toSignal(this.store.select(selectMonthlyExpense), { initialValue: null });
  readonly loadingDetail = toSignal(this.store.select(selectLoadingDetail), { initialValue: false });
  readonly loadingAccounts = toSignal(this.store.select(selectLoadingAccounts), { initialValue: false });
  readonly modalOpen = toSignal(this.store.select(selectModalOpen), { initialValue: false });
  readonly editingAccount = toSignal(this.store.select(selectEditingAccount), { initialValue: null });

  ngOnInit(): void {
    this.store.dispatch(AccountsActions.loadAccounts());
  }

  onAccountSelected(accountId: string): void {
    this.store.dispatch(AccountsActions.selectAccount({ accountId }));
  }

  onAddAccount(): void {
    this.store.dispatch(AccountsActions.openAddModal());
  }

  onEditAccount(account: Account): void {
    this.store.dispatch(AccountsActions.openEditModal({ account }));
  }

  onArchiveAccount(accountId: string): void {
    this.store.dispatch(AccountsActions.archiveAccount({ accountId }));
  }

  onDeleteAccount(accountId: string): void {
    this.confirmService.confirm({
      message: 'This account cannot be recovered. Continue?',
      header: 'Delete Account',
      acceptLabel: 'Delete',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.store.dispatch(AccountsActions.deleteAccount({ accountId })),
    });
  }

  onCloseModal(): void {
    this.store.dispatch(AccountsActions.closeModal());
  }
}
```

- [ ] **Step 2: Create accounts.component.html**

No `| async` — signals are called directly as functions.

```html
<div class="accounts-page">
  <header class="page-header">
    <h1 class="text-page-title">Accounts</h1>
    <button class="add-btn" (click)="onAddAccount()">＋ Add Account</button>
  </header>

  @if (accounts().length === 0 && !loadingAccounts()) {
    <div class="empty-page-state">
      <span class="empty-icon">🏦</span>
      <p class="empty-heading">No accounts yet</p>
      <p class="empty-sub">Add your first account to get started.</p>
      <button class="add-btn" (click)="onAddAccount()">＋ Add Account</button>
    </div>
  } @else {
    <div class="master-detail">
      <div class="left-panel">
        <app-account-list
          [accounts]="accounts()"
          [selectedAccountId]="selectedAccountId()"
          (accountSelected)="onAccountSelected($event)"
        />
      </div>
      <div class="detail-panel">
        <app-account-detail
          [account]="selectedAccount()"
          [transactions]="recentTransactions()"
          [monthlyIncome]="monthlyIncome()"
          [monthlyExpense]="monthlyExpense()"
          [loading]="loadingDetail()"
          (editClicked)="onEditAccount($event)"
          (archiveClicked)="onArchiveAccount($event)"
          (deleteClicked)="onDeleteAccount($event)"
        />
      </div>
    </div>
  }
</div>

<app-account-modal
  [visible]="modalOpen()"
  [account]="editingAccount()"
  (closed)="onCloseModal()"
/>

<p-confirmdialog appendTo="body" />
```

- [ ] **Step 3: Create accounts.component.scss**

```scss
@use '../../styles/variables' as *;

.accounts-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: $space-xl;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.add-btn {
  padding: $space-sm $space-lg;
  background: $color-primary;
  color: #fff;
  border: none;
  border-radius: $radius-md;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;

  &:hover {
    background: $color-primary-hover;
  }
}

.empty-page-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: $space-md;
  text-align: center;

  .empty-icon {
    font-size: 52px;
    opacity: 0.25;
  }

  .empty-heading {
    font-size: 16px;
    font-weight: 600;
    color: $color-text-primary;
    margin: 0;
  }

  .empty-sub {
    font-size: 13px;
    color: $color-text-muted;
    margin: 0;
  }
}

.master-detail {
  display: flex;
  gap: $space-xl;
  flex: 1;
  min-height: 0;
}

.left-panel {
  width: 260px;
  flex-shrink: 0;
  overflow-y: auto;
}

.detail-panel {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
}
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/features/accounts/accounts.component.ts frontend/src/app/features/accounts/accounts.component.html frontend/src/app/features/accounts/accounts.component.scss
git commit -m "feat: add AccountsComponent page shell with signals"
```

---

## Task 11: AccountListComponent (Left Panel)

**Signal strategy:** Replace `@Input` with `input()` / `input.required()` and `@Output EventEmitter` with `output()`. Template accesses inputs as `accounts()`, `selectedAccountId()`.

**Files:**
- Create: `frontend/src/app/features/accounts/account-list/account-list.component.ts`
- Create: `frontend/src/app/features/accounts/account-list/account-list.component.html`
- Create: `frontend/src/app/features/accounts/account-list/account-list.component.scss`

- [ ] **Step 1: Create account-list.component.ts**

```typescript
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CurrencyPipe } from '@angular/common';

import { Account } from '../../../core/models/account/account.model';
import { AccountTypePipe } from '../pipes/account-type.pipe';

@Component({
  selector: 'app-account-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, AccountTypePipe],
  templateUrl: './account-list.component.html',
  styleUrl: './account-list.component.scss',
})
export class AccountListComponent {
  readonly accounts = input.required<Account[]>();
  readonly selectedAccountId = input<string | null>(null);
  readonly accountSelected = output<string>();
}
```

- [ ] **Step 2: Create account-list.component.html**

Inputs are called as functions: `accounts()`, `selectedAccountId()`.

```html
<ul class="account-list">
  @for (account of accounts(); track account.id) {
    <li
      class="account-item"
      [class.selected]="account.id === selectedAccountId()"
      [class.archived]="account.archived"
      (click)="accountSelected.emit(account.id)"
    >
      <div class="accent-stripe" [style.background-color]="account.color ?? '#9ca3af'"></div>
      <div class="icon-circle">{{ account.icon ?? '🏦' }}</div>
      <div class="account-info">
        <span class="account-name">{{ account.name }}</span>
        <span class="account-meta">{{ account.type | accountType }} · {{ account.currency }}</span>
      </div>
      <span class="account-balance">
        {{ account.balance | currency: account.currency : 'symbol-narrow' : '1.2-2' }}
      </span>
    </li>
  }
</ul>
```

- [ ] **Step 3: Create account-list.component.scss**

```scss
@use '../../../../styles/variables' as *;

.account-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: $space-sm;
}

.account-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: $space-md;
  padding: $space-md $space-md $space-md $space-xl;
  background: $color-bg-card;
  border: 1px solid $color-border;
  border-radius: $radius-md;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.15s;

  &.selected {
    border-color: $color-primary;
    background: $color-primary-faint;
  }

  &.archived {
    opacity: 0.45;
  }

  &:hover:not(.selected) {
    border-color: $color-border-strong;
  }
}

.accent-stripe {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
}

.icon-circle {
  width: 34px;
  height: 34px;
  border-radius: $radius-pill;
  background: $color-bg-muted;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.account-info {
  flex: 1;
  min-width: 0;
}

.account-name {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: $color-text-primary;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.account-meta {
  display: block;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: $color-text-muted;
  margin-top: 2px;
}

.account-balance {
  font-size: 13px;
  font-weight: 700;
  color: $color-text-primary;
  white-space: nowrap;
  flex-shrink: 0;
}
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/features/accounts/account-list/
git commit -m "feat: add AccountListComponent left panel with signal inputs"
```

---

## Task 12: AccountDetailComponent (Right Panel)

**Signal strategy:** Same `input()`/`output()` approach as AccountListComponent. The `menuItems` getter reads `this.account()` since it's a signal input.

**Files:**
- Create: `frontend/src/app/features/accounts/account-detail/account-detail.component.ts`
- Create: `frontend/src/app/features/accounts/account-detail/account-detail.component.html`
- Create: `frontend/src/app/features/accounts/account-detail/account-detail.component.scss`

- [ ] **Step 1: Create account-detail.component.ts**

```typescript
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Menu } from 'primeng/menu';
import { MenuItem } from 'primeng/api';

import { Account } from '../../../core/models/account/account.model';
import { TransactionResponse } from '../../../core/models/transaction/transaction-response.model';
import { AccountTypePipe } from '../pipes/account-type.pipe';

@Component({
  selector: 'app-account-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, DatePipe, RouterLink, Menu, AccountTypePipe],
  templateUrl: './account-detail.component.html',
  styleUrl: './account-detail.component.scss',
})
export class AccountDetailComponent {
  readonly account = input<Account | null>(null);
  readonly transactions = input<TransactionResponse[]>([]);
  readonly monthlyIncome = input<number | null>(null);
  readonly monthlyExpense = input<number | null>(null);
  readonly loading = input(false);
  readonly editClicked = output<Account>();
  readonly archiveClicked = output<string>();
  readonly deleteClicked = output<string>();

  get menuItems(): MenuItem[] {
    const account = this.account();
    if (!account) return [];
    return [
      { label: '✏️ Edit account', command: () => this.editClicked.emit(account) },
      { label: '📦 Archive', command: () => this.archiveClicked.emit(account.id) },
      {
        label: '🗑 Delete',
        command: () => this.deleteClicked.emit(account.id),
        styleClass: 'danger-menu-item',
      },
    ];
  }

  txTypeClass(type: string): string {
    return type.toLowerCase();
  }

  txIcon(type: string): string {
    if (type === 'INCOME') return '🔼';
    if (type === 'EXPENSE') return '🔽';
    return '↔️';
  }

  txLabel(tx: TransactionResponse): string {
    return tx.categoryName ?? tx.note ?? 'Transaction';
  }
}
```

- [ ] **Step 2: Create account-detail.component.html**

`@if (account(); as acc)` — if account() is null the block is skipped; otherwise `acc` holds the Account value.

```html
@if (!account()) {
  <div class="detail-empty">
    <span class="empty-icon">🏦</span>
    <p class="empty-title">Select an account</p>
    <p class="empty-subtitle">
      Click an account on the left to see its details and recent transactions.
    </p>
  </div>
} @else if (account(); as acc) {
  <div class="detail-card">
    <div class="detail-header" [style.border-left-color]="acc.color ?? '#9ca3af'">
      <div class="icon-circle">{{ acc.icon ?? '🏦' }}</div>
      <div class="header-info">
        <div class="account-name">{{ acc.name }}</div>
        <div class="account-sub">
          {{ acc.type | accountType }} · {{ acc.currency }}
          @if (acc.archived) { · <em>Archived</em> }
        </div>
      </div>
      <div class="header-balance">
        {{ acc.balance | currency: acc.currency : 'symbol-narrow' : '1.2-2' }}
      </div>
      <button class="overflow-btn" type="button" (click)="menu.toggle($event)">⋯</button>
      <p-menu #menu [model]="menuItems" [popup]="true" appendTo="body" />
    </div>

    <div class="detail-body">
      <div class="stats-strip">
        <div class="stat-box">
          <div class="stat-label">Income this month</div>
          <div class="stat-amount income">
            {{ (monthlyIncome() ?? 0) | currency: acc.currency : 'symbol-narrow' : '1.2-2' }}
          </div>
        </div>
        <div class="stat-box">
          <div class="stat-label">Expenses this month</div>
          <div class="stat-amount expense">
            {{ (monthlyExpense() ?? 0) | currency: acc.currency : 'symbol-narrow' : '1.2-2' }}
          </div>
        </div>
      </div>

      <div class="transactions-section">
        <div class="section-title">Recent Transactions</div>

        @if (loading()) {
          <p class="tx-placeholder">Loading…</p>
        } @else if (transactions().length === 0) {
          <p class="tx-placeholder">No recent transactions.</p>
        } @else {
          @for (tx of transactions(); track tx.id) {
            <div class="transaction-row">
              <div class="tx-icon" [class]="txTypeClass(tx.type)">{{ txIcon(tx.type) }}</div>
              <div class="tx-details">
                <div class="tx-description">{{ txLabel(tx) }}</div>
                <div class="tx-date">{{ tx.transactionDate | date: 'MMM d, y' }}</div>
              </div>
              <div class="tx-amount" [class]="txTypeClass(tx.type)">
                {{ tx.amount | currency: tx.currency : 'symbol-narrow' : '1.2-2' }}
              </div>
            </div>
          }
        }

        <a
          class="view-all-btn"
          [routerLink]="['/transactions']"
          [queryParams]="{ accountId: acc.id }"
        >View all transactions →</a>
      </div>
    </div>
  </div>
}
```

- [ ] **Step 3: Create account-detail.component.scss**

```scss
@use '../../../../styles/variables' as *;

.detail-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: $space-md;
  text-align: center;

  .empty-icon {
    font-size: 52px;
    opacity: 0.25;
  }

  .empty-title {
    font-size: 17px;
    font-weight: 600;
    color: $color-text-secondary;
    margin: 0;
  }

  .empty-subtitle {
    font-size: 13px;
    color: $color-text-muted;
    max-width: 240px;
    line-height: 1.5;
    margin: 0;
  }
}

.detail-card {
  background: $color-bg-card;
  border: 1px solid $color-border;
  border-radius: $radius-lg;
  overflow: hidden;
  box-shadow: $shadow-sm;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: $space-md;
  padding: $space-lg;
  border-left: 4px solid $color-text-muted;

  .icon-circle {
    width: 42px;
    height: 42px;
    border-radius: $radius-pill;
    background: $color-bg-muted;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    flex-shrink: 0;
  }

  .header-info {
    flex: 1;
    min-width: 0;

    .account-name {
      font-size: 16px;
      font-weight: 700;
      color: $color-text-primary;
    }

    .account-sub {
      font-size: 12px;
      color: $color-text-secondary;
      margin-top: 2px;
    }
  }

  .header-balance {
    font-size: 24px;
    font-weight: 700;
    color: $color-text-primary;
    margin-left: auto;
    white-space: nowrap;
  }

  .overflow-btn {
    width: 34px;
    height: 34px;
    border-radius: $radius-md;
    background: $color-bg-muted;
    border: none;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    color: $color-text-secondary;
    flex-shrink: 0;

    &:hover {
      background: $color-border;
    }
  }
}

.detail-body {
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: $space-xl;
}

.stats-strip {
  display: flex;
  gap: $space-md;

  .stat-box {
    flex: 1;
    background: $color-bg-muted;
    border-radius: $radius-md;
    padding: $space-md;

    .stat-label {
      font-size: 11px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      color: $color-text-muted;
      margin-bottom: $space-xs;
    }

    .stat-amount {
      font-size: 17px;
      font-weight: 700;

      &.income { color: $color-income; }
      &.expense { color: $color-expense; }
    }
  }
}

.transactions-section {
  display: flex;
  flex-direction: column;

  .section-title {
    font-size: 10px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.06em;
    color: $color-text-muted;
    margin-bottom: $space-md;
  }

  .tx-placeholder {
    font-size: 13px;
    color: $color-text-muted;
    margin: $space-md 0;
  }
}

.transaction-row {
  display: flex;
  align-items: center;
  gap: $space-md;
  padding: $space-sm 0;

  & + & {
    border-top: 1px solid #f1f5f9;
  }

  .tx-icon {
    width: 30px;
    height: 30px;
    border-radius: $radius-sm;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    flex-shrink: 0;

    &.income { background: $color-income-faint; }
    &.expense { background: $color-expense-faint; }
    &.transfer { background: $color-transfer-faint; }
  }

  .tx-details {
    flex: 1;
    min-width: 0;

    .tx-description {
      font-size: 13px;
      font-weight: 500;
      color: $color-text-body;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .tx-date {
      font-size: 11px;
      color: $color-text-muted;
      margin-top: 2px;
    }
  }

  .tx-amount {
    font-size: 13px;
    font-weight: 700;
    white-space: nowrap;
    flex-shrink: 0;

    &.income { color: $color-income; }
    &.expense { color: $color-expense; }
    &.transfer { color: $color-transfer; }
  }
}

.view-all-btn {
  display: block;
  width: 100%;
  padding: 10px $space-lg;
  margin-top: $space-md;
  background: $color-bg-muted;
  color: $color-primary;
  border: none;
  border-radius: $radius-md;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
  text-decoration: none;
  cursor: pointer;
  box-sizing: border-box;

  &:hover {
    background: $color-border;
  }
}

::ng-deep .danger-menu-item .p-menuitem-link .p-menuitem-text {
  color: $color-expense;
}
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/features/accounts/account-detail/
git commit -m "feat: add AccountDetailComponent right panel with signal inputs"
```

---

## Task 13: AccountModalComponent (Add/Edit Dialog)

**Signal strategy:**
- `visible` and `account` are `input()` signals.
- `saving` and `saveError` are converted from NgRx via `toSignal()`.
- `userSignal` (for default currency) is also a `toSignal()` field.
- `effect()` with `untracked()` replaces `ngOnChanges`: the effect re-runs only when `visible()` changes to `true`; `untracked()` reads `account()` and `userSignal()` without creating additional dependencies.
- No `AsyncPipe` — `saving()` and `saveError()` are called directly in the template.

**Files:**
- Create: `frontend/src/app/features/accounts/account-modal/account-modal.component.ts`
- Create: `frontend/src/app/features/accounts/account-modal/account-modal.component.html`
- Create: `frontend/src/app/features/accounts/account-modal/account-modal.component.scss`

- [ ] **Step 1: Create account-modal.component.ts**

```typescript
import {
  ChangeDetectionStrategy,
  Component,
  effect,
  inject,
  input,
  output,
  untracked,
} from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { Store } from '@ngrx/store';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Select } from 'primeng/select';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';

import { AccountsActions } from '../store/accounts.actions';
import { selectSaveError, selectSaving } from '../store/accounts.reducer';
import { selectUser } from '../../auth/store/auth.reducer';
import { Account } from '../../../core/models/account/account.model';
import { AccountRequest } from '../../../core/models/account/account-request.model';

const PRESET_COLORS = [
  '#ef4444', '#f97316', '#eab308', '#22c55e',
  '#14b8a6', '#3b82f6', '#8b5cf6', '#ec4899',
  '#6b7280', '#0ea5e9', '#84cc16', '#f43f5e',
];

const PRESET_ICONS = ['💰', '💳', '🏦', '🐷', '📈', '💵', '🏠', '🚗', '✈️', '🎓', '🏥', '💼'];

const ACCOUNT_TYPES = [
  { label: 'Cash', value: 'CASH' },
  { label: 'Bank', value: 'BANK' },
  { label: 'Credit Card', value: 'CREDIT_CARD' },
  { label: 'Savings', value: 'SAVINGS' },
  { label: 'Investment', value: 'INVESTMENT' },
  { label: 'Other', value: 'OTHER' },
];

const CURRENCIES = [
  { label: 'EUR - Euro', value: 'EUR' },
  { label: 'USD - US Dollar', value: 'USD' },
  { label: 'GBP - British Pound', value: 'GBP' },
  { label: 'JPY - Japanese Yen', value: 'JPY' },
  { label: 'CHF - Swiss Franc', value: 'CHF' },
  { label: 'CAD - Canadian Dollar', value: 'CAD' },
  { label: 'AUD - Australian Dollar', value: 'AUD' },
  { label: 'CNY - Chinese Yuan', value: 'CNY' },
  { label: 'INR - Indian Rupee', value: 'INR' },
  { label: 'BRL - Brazilian Real', value: 'BRL' },
  { label: 'SEK - Swedish Krona', value: 'SEK' },
  { label: 'NOK - Norwegian Krone', value: 'NOK' },
  { label: 'DKK - Danish Krone', value: 'DKK' },
  { label: 'PLN - Polish Złoty', value: 'PLN' },
  { label: 'SGD - Singapore Dollar', value: 'SGD' },
  { label: 'HKD - Hong Kong Dollar', value: 'HKD' },
  { label: 'KRW - South Korean Won', value: 'KRW' },
  { label: 'ZAR - South African Rand', value: 'ZAR' },
  { label: 'TRY - Turkish Lira', value: 'TRY' },
];

@Component({
  selector: 'app-account-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, Dialog, InputText, InputNumber, Select, Button, Message],
  templateUrl: './account-modal.component.html',
  styleUrl: './account-modal.component.scss',
})
export class AccountModalComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly store = inject(Store);

  readonly visible = input.required<boolean>();
  readonly account = input<Account | null>(null);
  readonly closed = output<void>();

  readonly saving = toSignal(this.store.select(selectSaving), { initialValue: false });
  readonly saveError = toSignal(this.store.select(selectSaveError), { initialValue: null });
  private readonly user = toSignal(this.store.select(selectUser));

  readonly form = this.fb.group({
    name: this.fb.control('', [Validators.required, Validators.maxLength(100)]),
    type: this.fb.control('CASH', [Validators.required]),
    currency: this.fb.control('EUR', [Validators.required]),
    initialBalance: this.fb.control<number | null>(0),
    color: this.fb.control<string | null>(PRESET_COLORS[4]),
    icon: this.fb.control<string | null>(PRESET_ICONS[2]),
  });

  readonly presetColors = PRESET_COLORS;
  readonly presetIcons = PRESET_ICONS;
  readonly accountTypes = ACCOUNT_TYPES;
  readonly currencies = CURRENCIES;

  get isEditMode(): boolean { return this.account() !== null; }
  get title(): string { return this.isEditMode ? 'Edit Account' : 'Add Account'; }

  constructor() {
    // Reset/populate form whenever the dialog opens.
    // untracked() reads account() and user() without creating dependencies
    // so the effect only re-runs when visible() changes.
    effect(() => {
      if (!this.visible()) return;
      untracked(() => {
        const acc = this.account();
        if (acc) {
          this.form.patchValue({
            name: acc.name,
            type: acc.type,
            currency: acc.currency,
            color: acc.color,
            icon: acc.icon,
          });
        } else {
          this.form.reset({
            name: '',
            type: 'CASH',
            currency: this.user()?.homeCurrency ?? 'EUR',
            initialBalance: 0,
            color: PRESET_COLORS[4],
            icon: PRESET_ICONS[2],
          });
        }
      });
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { name, type, currency, initialBalance, color, icon } = this.form.getRawValue();
    const accountId = this.account()?.id;
    const request: AccountRequest = {
      name, type, currency, color, icon,
      ...(!accountId && { initialBalance: initialBalance ?? 0 }),
    };
    this.store.dispatch(AccountsActions.saveAccount({ request, ...(accountId && { accountId }) }));
  }

  close(): void {
    this.closed.emit();
  }
}
```

- [ ] **Step 2: Create account-modal.component.html**

No `| async` — `saving()` and `saveError()` are signals called directly.

```html
<p-dialog
  [visible]="visible()"
  [header]="title"
  [modal]="true"
  [style]="{ width: '500px' }"
  styleClass="account-modal"
  [draggable]="false"
  [resizable]="false"
  appendTo="body"
  (onHide)="close()"
>
  @if (saveError()) {
    <p-message severity="error">{{ saveError() }}</p-message>
  }

  <form [formGroup]="form" (ngSubmit)="submit()" novalidate class="modal-form">
    <div class="field">
      <label for="acc-name" class="field-label">Name *</label>
      <input
        id="acc-name"
        type="text"
        pInputText
        formControlName="name"
        placeholder="e.g. Main Wallet"
        [fluid]="true"
        [invalid]="form.controls.name.invalid && form.controls.name.touched"
      />
      @if (form.controls.name.touched && form.controls.name.hasError('required')) {
        <small class="field-error">Name is required.</small>
      } @else if (form.controls.name.touched && form.controls.name.hasError('maxlength')) {
        <small class="field-error">Name must be 100 characters or fewer.</small>
      }
    </div>

    <div class="form-row">
      <div class="field">
        <label class="field-label">Type *</label>
        <p-select
          [options]="accountTypes"
          formControlName="type"
          optionLabel="label"
          optionValue="value"
          [fluid]="true"
        />
      </div>
      <div class="field">
        <label class="field-label">Currency *</label>
        <p-select
          [options]="currencies"
          formControlName="currency"
          optionLabel="label"
          optionValue="value"
          [filter]="true"
          filterBy="label"
          [fluid]="true"
        />
      </div>
    </div>

    @if (!isEditMode) {
      <div class="field">
        <label for="acc-balance" class="field-label">Initial Balance</label>
        <p-inputnumber
          inputId="acc-balance"
          formControlName="initialBalance"
          [minFractionDigits]="2"
          [maxFractionDigits]="2"
          [fluid]="true"
        />
      </div>
    }

    <div class="field">
      <label class="field-label">Color</label>
      <div
        class="color-preview-bar"
        [style.background-color]="form.controls.color.value ?? '#9ca3af'"
      ></div>
      <div class="color-grid">
        @for (color of presetColors; track color) {
          <button
            type="button"
            class="color-swatch"
            [class.selected]="form.controls.color.value === color"
            [style.background-color]="color"
            (click)="form.controls.color.setValue(color)"
          ></button>
        }
      </div>
    </div>

    <div class="field">
      <label class="field-label">Icon</label>
      <div class="icon-grid">
        @for (icon of presetIcons; track icon) {
          <button
            type="button"
            class="icon-swatch"
            [class.selected]="form.controls.icon.value === icon"
            (click)="form.controls.icon.setValue(icon)"
          >{{ icon }}</button>
        }
      </div>
    </div>

    <div class="modal-footer">
      <p-button type="button" label="Cancel" severity="secondary" (click)="close()" />
      <p-button type="submit" label="Save" [loading]="saving()" />
    </div>
  </form>
</p-dialog>
```

- [ ] **Step 3: Create account-modal.component.scss**

```scss
@use '../../../../styles/variables' as *;

.modal-form {
  display: flex;
  flex-direction: column;
  gap: $space-lg;
  padding-top: $space-sm;
}

.form-row {
  display: flex;
  gap: $space-lg;

  > .field { flex: 1; }
}

.field {
  display: flex;
  flex-direction: column;
  gap: $space-sm;

  .field-label {
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: $color-text-secondary;
  }

  .field-error {
    font-size: 11px;
    color: $color-expense;
  }
}

.color-preview-bar {
  height: 4px;
  border-radius: $radius-sm;
  transition: background-color 0.15s;
}

.color-grid,
.icon-grid {
  display: flex;
  flex-wrap: wrap;
  gap: $space-sm;
}

.color-swatch {
  width: 28px;
  height: 28px;
  border-radius: $radius-sm;
  border: 2px solid transparent;
  cursor: pointer;
  padding: 0;
  transition: transform 0.1s;

  &.selected {
    border-color: $color-text-primary;
    transform: scale(1.15);
  }

  &:hover:not(.selected) {
    transform: scale(1.1);
  }
}

.icon-swatch {
  width: 36px;
  height: 36px;
  border-radius: $radius-sm;
  background: $color-bg-muted;
  border: 2px solid transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  transition: border-color 0.1s, background-color 0.1s;

  &.selected {
    border-color: $color-primary;
    background: $color-primary-faint;
  }

  &:hover:not(.selected) {
    background: $color-border;
  }
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: $space-md;
  padding-top: $space-lg;
  border-top: 1px solid $color-border;
}
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/features/accounts/account-modal/
git commit -m "feat: add AccountModalComponent add/edit dialog with signals"
```

---

## Task 14: Verification

- [ ] **Step 1: Run all frontend tests**

```bash
cd frontend && ng test --watch=false 2>&1 | tail -20
```

Expected: All tests pass. Fix any failures before manual verification.

- [ ] **Step 2: Start the dev environment**

```bash
# From project root:
docker compose up -d postgres
# In a second terminal from backend/:
mvn spring-boot:run
# In a third terminal from frontend/:
ng serve
```

- [ ] **Step 3: Verify in browser at http://localhost:4200/accounts**

| Check | Expected result |
|-------|----------------|
| Page loads | "Accounts" heading + "＋ Add Account" button visible |
| Zero accounts | 🏦 empty state: "No accounts yet" + button; no list/detail panels |
| Open modal (+ button) | Dialog opens; currency defaults to user's `homeCurrency` |
| Form validation | Submit with empty name shows "Name is required." |
| Create account | Fill all fields, Save → account appears in left panel |
| Account item | Accent stripe in chosen color, icon circle, name, type · currency, balance |
| Select account | Teal border + faint background; detail panel shows name, balance, stats |
| Monthly stats | Income green / Expenses red |
| Recent transactions | Up to 5 rows with icon, description, date, colored amount |
| Overflow menu ⋯ | Edit / Archive / Delete (Delete text is red) |
| Edit account | Form pre-populated; initial balance field absent; save updates list |
| Archive | Account dims to 0.45 opacity, panel closes |
| Delete | Confirm dialog appears; confirming removes account from list |
| "View all transactions →" | Link points to `/transactions?accountId=<id>` |
