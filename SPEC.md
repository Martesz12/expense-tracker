# Technical Specification – Expense Tracker Web Application

## 1. Overview

A full-stack web application for personal finance management. Authenticated users can track income and expenses across multiple accounts, organize transactions by category, manage budgets, and view rich reports and statistics. Inspired by the 1Money mobile app — bringing the same core experience to the web.

### Core Features

- User authentication (sign up, login, logout)
- Multiple financial accounts (cash, bank, credit card, savings, etc.)
- Transaction management: income, expense, and transfer transactions
- Category management with custom icons and colors
- Budget planning and progress tracking
- Reports and statistics with charts (by period, category, account)
- Multi-currency support with exchange rates
- Recurring / scheduled transactions
- Notes and tags on transactions
- Data export (CSV)

### Tech

- **Frontend:** Angular (latest – v21) + SCSS + PrimeNG + NgRx
- **Backend:** Spring Boot 3.x (Java 21)
- **Database:** PostgreSQL 16
- **Auth:** Spring Security with JWT (access + refresh tokens)
- **Build tools:** Maven (backend), Angular CLI (frontend)

---

## 2. Architecture

### 2.1 High-Level Architecture

```
┌─────────────────────────────────┐     HTTP/REST     ┌──────────────────────────────────┐
│         Angular Frontend        │ ◄────────────────► │       Spring Boot Backend        │
│  (SPA, served via nginx/Node)   │                    │  (REST API, port 8080)           │
└─────────────────────────────────┘                    └────────────────┬─────────────────┘
                                                                        │
                                                                        ▼
                                                          ┌─────────────────────────┐
                                                          │     PostgreSQL 16        │
                                                          │     (port 5432)          │
                                                          └─────────────────────────┘
```

- **Frontend:** Angular SPA communicating with the backend exclusively via REST JSON APIs. All protected routes require a valid JWT.
- **Backend:** Spring Boot REST API. Stateless — authentication is handled via JWT tokens stored on the client.
- **Database:** PostgreSQL accessed via Spring Data JPA (Hibernate). Migrations managed by Flyway.

### 2.2 Application Layers

**Frontend**
- Angular standalone components with lazy-loaded feature modules
- NgRx for global state (auth, accounts, transactions, categories, budgets)
- PrimeNG for UI components (tables, dialogs, charts, dropdowns, calendar, etc.)
- SCSS with a design token–based theming layer

**Backend**
- Controller layer: REST endpoints (`@RestController`)
- Service layer: business logic
- Repository layer: Spring Data JPA repositories
- Security layer: JWT filter chain, method-level `@PreAuthorize`

**Database**
- PostgreSQL with Flyway migrations under `resources/db/migration`

---

## 3. Functional Requirements

### 3.1 Authentication

Users can:
- Register with name, email, and password
- Log in and receive a JWT access token + refresh token
- Refresh access tokens silently using the refresh token
- Log out (invalidates refresh token server-side)

Password requirements: minimum 8 characters.

Unauthenticated users:
- Can only access `/login` and `/register`
- All other routes redirect to `/login`

### 3.2 Accounts

Users manage one or more financial accounts. Each account has:
- Name (e.g., "Wallet", "Main Bank Account")
- Type: `CASH`, `BANK`, `CREDIT_CARD`, `SAVINGS`, `INVESTMENT`, `OTHER`
- Currency (ISO 4217 code, e.g., `HUF`, `EUR`, `USD`)
- Initial balance (used to compute running balance)
- Color + icon (for visual identification)
- Active / archived status

**Operations:**
- Create, edit, delete (soft-delete / archive)
- View current balance (computed from initial balance + all transactions)
- View per-account transaction history

### 3.3 Transactions

A transaction always belongs to one user and references one or two accounts (transfers).

**Transaction types:**
- `EXPENSE` – money leaving an account
- `INCOME` – money entering an account
- `TRANSFER` – money moving between two accounts owned by the same user

**Fields per transaction:**
- Type
- Amount (positive decimal)
- Currency (defaults to account currency)
- Date & time
- Category (required for EXPENSE and INCOME; not applicable for TRANSFER)
- From account / To account
- Note (optional free text)
- Tags (optional, many-to-many)
- Recurring rule reference (optional)

**Operations:**
- Create, edit, delete
- List with filters: date range, account, category, type, tag, text search
- Pagination (server-side)

### 3.4 Categories

Categories classify income and expense transactions. They are user-scoped (each user has their own).

A set of default categories is created when a user registers (e.g., Food, Transport, Housing, Entertainment, Health, Shopping, Salary, Freelance).

**Fields:**
- Name
- Type: `EXPENSE`, `INCOME`, or `BOTH`
- Icon (from a predefined icon set or emoji)
- Color (hex)
- Parent category (optional, for subcategories one level deep)

**Operations:**
- Create, edit, delete (only if no transactions reference it)
- List own categories

### 3.5 Budgets

Users can define monthly (or custom-period) spending budgets per category.

**Fields:**
- Category
- Period type: `MONTHLY`, `WEEKLY`, `CUSTOM`
- Period start / end (for custom)
- Limit amount
- Currency

**Operations:**
- Create, edit, delete
- View progress: spent vs. limit, percentage, remaining
- List active budgets for the current period

### 3.6 Recurring Transactions

Users can set up recurring rules that auto-generate transactions on a schedule.

**Fields:**
- Base transaction template (all transaction fields)
- Frequency: `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`
- Interval (e.g., every 2 weeks)
- Start date
- End date (optional)
- Next occurrence date (managed by backend scheduler)

A Spring `@Scheduled` job runs daily to materialize due recurring transactions.

### 3.7 Reports & Statistics

Read-only views that aggregate transaction data:

- **Overview / Dashboard:**
  - Total income, total expenses, net balance for the current month
  - Balance trend line chart (last 6 months)
  - Expense breakdown donut chart (by category, current month)
  - Recent transactions list

- **Expense by Category:**
  - Configurable date range
  - Bar or donut chart
  - Table with category name, amount, percentage

- **Income vs. Expense:**
  - Bar chart grouped by month
  - Configurable number of months

- **Account Balances:**
  - Table/card view of all accounts with current balance
  - Balance history line chart per account

### 3.8 Multi-Currency

- Each account has a base currency
- Transactions can optionally be recorded in a different currency with an exchange rate field
- Reports display amounts converted to a user-selected "home currency"
- Exchange rates are manually entered per transaction (no live rate API required, but the architecture should make it easy to add one later)

### 3.9 Tags

Free-form tags can be attached to any transaction for cross-cutting queries (e.g., "vacation", "work", "reimbursable").

- Create tags inline while adding a transaction
- Filter transaction list by one or more tags

### 3.10 Data Export

- Export transactions to CSV
- Filters (date range, account, category) apply before export
- File downloaded directly from the backend

---

## 4. Non-Functional Requirements

**Performance**
- Transaction list (paginated, 50 rows) must respond in < 300 ms for typical data sizes (up to ~50k transactions per user)
- Dashboard aggregation queries must respond in < 500 ms; use database indexes accordingly

**Security**
- All endpoints (except `/api/auth/**`) require a valid JWT
- All queries are scoped to the authenticated user's ID — no cross-user data leakage
- Passwords stored as bcrypt hashes
- Refresh tokens are stored hashed in the DB and rotated on each use

**Reliability**
- Transactional integrity for transfers (debit + credit in a single DB transaction)
- Graceful error handling with consistent JSON error responses

**Maintainability**
- Flyway for DB migrations — no manual schema changes
- DTOs separate from JPA entities
- NgRx effects isolate all side effects (API calls) from components

**UX**
- Responsive layout (desktop-first, usable on tablets)
- PrimeNG theme customizable via SCSS variables
- Loading spinners / skeletons on async operations
- Toast notifications for success/error feedback

---

## 5. Data Model & Database Schema (PostgreSQL)

### 5.1 Tables

#### users

```sql
CREATE TABLE users (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name        VARCHAR(100) NOT NULL,
  email       VARCHAR(255) NOT NULL UNIQUE,
  password    VARCHAR(255) NOT NULL,  -- bcrypt hash
  home_currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

#### refresh_tokens

```sql
CREATE TABLE refresh_tokens (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash  VARCHAR(255) NOT NULL UNIQUE,  -- SHA-256 of the raw token
  expires_at  TIMESTAMPTZ NOT NULL,
  revoked     BOOLEAN NOT NULL DEFAULT FALSE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

#### accounts

```sql
CREATE TABLE accounts (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name            VARCHAR(100) NOT NULL,
  type            VARCHAR(20) NOT NULL,  -- CASH, BANK, CREDIT_CARD, SAVINGS, INVESTMENT, OTHER
  currency        VARCHAR(3) NOT NULL,
  initial_balance NUMERIC(15, 2) NOT NULL DEFAULT 0,
  color           VARCHAR(7),            -- hex color, e.g. #4CAF50
  icon            VARCHAR(50),           -- icon identifier
  is_archived     BOOLEAN NOT NULL DEFAULT FALSE,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

#### categories

```sql
CREATE TABLE categories (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name        VARCHAR(100) NOT NULL,
  type        VARCHAR(10) NOT NULL,  -- EXPENSE, INCOME, BOTH
  icon        VARCHAR(50),
  color       VARCHAR(7),
  parent_id   UUID REFERENCES categories(id) ON DELETE SET NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

#### tags

```sql
CREATE TABLE tags (
  id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name    VARCHAR(50) NOT NULL,
  UNIQUE (user_id, name)
);
```

#### recurring_rules

```sql
CREATE TABLE recurring_rules (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  description     VARCHAR(255),
  type            VARCHAR(10) NOT NULL,  -- EXPENSE, INCOME, TRANSFER
  amount          NUMERIC(15, 2) NOT NULL,
  currency        VARCHAR(3) NOT NULL,
  from_account_id UUID NOT NULL REFERENCES accounts(id),
  to_account_id   UUID REFERENCES accounts(id),  -- only for TRANSFER
  category_id     UUID REFERENCES categories(id),
  note            TEXT,
  frequency       VARCHAR(10) NOT NULL,  -- DAILY, WEEKLY, MONTHLY, YEARLY
  interval_value  INTEGER NOT NULL DEFAULT 1,
  start_date      DATE NOT NULL,
  end_date        DATE,
  next_occurrence DATE NOT NULL,
  is_active       BOOLEAN NOT NULL DEFAULT TRUE,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

#### transactions

```sql
CREATE TABLE transactions (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type             VARCHAR(10) NOT NULL,  -- EXPENSE, INCOME, TRANSFER
  amount           NUMERIC(15, 2) NOT NULL,
  currency         VARCHAR(3) NOT NULL,
  exchange_rate    NUMERIC(15, 6),        -- NULL means same currency as account
  from_account_id  UUID NOT NULL REFERENCES accounts(id),
  to_account_id    UUID REFERENCES accounts(id),  -- only for TRANSFER
  category_id      UUID REFERENCES categories(id),
  note             TEXT,
  transaction_date TIMESTAMPTZ NOT NULL,
  recurring_rule_id UUID REFERENCES recurring_rules(id) ON DELETE SET NULL,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

#### transaction_tags

```sql
CREATE TABLE transaction_tags (
  transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
  tag_id         UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
  PRIMARY KEY (transaction_id, tag_id)
);
```

#### budgets

```sql
CREATE TABLE budgets (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  category_id  UUID NOT NULL REFERENCES categories(id),
  period_type  VARCHAR(10) NOT NULL,  -- MONTHLY, WEEKLY, CUSTOM
  period_start DATE,                  -- for CUSTOM
  period_end   DATE,                  -- for CUSTOM
  amount_limit NUMERIC(15, 2) NOT NULL,
  currency     VARCHAR(3) NOT NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### 5.2 Indexes

```sql
-- Transactions: most queried table
CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_date ON transactions(user_id, transaction_date DESC);
CREATE INDEX idx_transactions_from_account ON transactions(from_account_id);
CREATE INDEX idx_transactions_to_account ON transactions(to_account_id);
CREATE INDEX idx_transactions_category ON transactions(category_id);

-- Accounts
CREATE INDEX idx_accounts_user_id ON accounts(user_id);

-- Categories
CREATE INDEX idx_categories_user_id ON categories(user_id);

-- Budgets
CREATE INDEX idx_budgets_user_id ON budgets(user_id);

-- Refresh tokens
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- Recurring rules
CREATE INDEX idx_recurring_rules_next_occurrence ON recurring_rules(next_occurrence)
  WHERE is_active = TRUE;
```

---

## 6. Backend: Spring Boot API

### 6.1 Project Structure

```
src/main/java/com/yourapp/
├── config/
│   ├── SecurityConfig.java
│   ├── JwtConfig.java
│   └── SchedulerConfig.java
├── auth/
│   ├── AuthController.java
│   ├── AuthService.java
│   ├── JwtUtil.java
│   └── JwtAuthFilter.java
├── user/
│   ├── User.java              (JPA entity)
│   ├── UserRepository.java
│   └── UserService.java
├── account/
│   ├── Account.java
│   ├── AccountRepository.java
│   ├── AccountService.java
│   ├── AccountController.java
│   └── dto/
├── transaction/
│   ├── Transaction.java
│   ├── TransactionRepository.java
│   ├── TransactionService.java
│   ├── TransactionController.java
│   ├── TransactionExportService.java
│   └── dto/
├── category/
├── budget/
├── tag/
├── recurring/
│   ├── RecurringRule.java
│   ├── RecurringRuleRepository.java
│   ├── RecurringRuleService.java
│   ├── RecurringRuleController.java
│   └── RecurringScheduler.java
├── report/
│   ├── ReportController.java
│   └── ReportService.java
└── common/
    ├── exception/
    │   ├── GlobalExceptionHandler.java
    │   └── ResourceNotFoundException.java
    └── dto/
        ├── ApiResponse.java
        └── PageResponse.java
```

### 6.2 Security Configuration

- Spring Security filter chain with stateless session management
- `JwtAuthFilter` (extends `OncePerRequestFilter`) validates Bearer token from `Authorization` header
- Public endpoints: `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/refresh`
- All others require authentication
- Authenticated user's `UUID` extracted from JWT and available via `SecurityContextHolder`

### 6.3 JWT Strategy

- **Access token:** short-lived (15 minutes), signed with HS256
- **Refresh token:** long-lived (30 days), stored hashed in `refresh_tokens` table
- On refresh: validate raw token against stored hash, rotate (issue new pair, revoke old)
- On logout: mark refresh token as revoked

### 6.4 Service Layer Conventions

- All service methods accept `UUID userId` as first parameter to enforce data scoping
- `@Transactional` on write operations
- Transfer transactions: debit and credit recorded atomically
- Balance computation: `initial_balance + SUM(income) - SUM(expense)` — no denormalized balance column (keep it simple; add caching later if needed)

---

## 7. API Design

All endpoints are prefixed with `/api`. Requests and responses use JSON. Error responses follow a consistent structure:

```json
{
  "status": 400,
  "error": "Validation failed",
  "message": "amount must be greater than 0",
  "timestamp": "2025-01-01T12:00:00Z"
}
```

### 7.1 Auth

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login, receive access + refresh tokens |
| POST | `/api/auth/refresh` | Exchange refresh token for new pair |
| POST | `/api/auth/logout` | Revoke refresh token |

**POST /api/auth/register – request:**
```json
{ "name": "Jane Doe", "email": "jane@example.com", "password": "secret123" }
```

**POST /api/auth/login – response:**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "uuid-v4-raw-token",
  "user": { "id": "...", "name": "Jane Doe", "email": "jane@example.com", "homeCurrency": "EUR" }
}
```

### 7.2 Accounts

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/accounts` | List all accounts (active + archived) |
| POST | `/api/accounts` | Create account |
| GET | `/api/accounts/:id` | Get account detail + current balance |
| PUT | `/api/accounts/:id` | Update account |
| DELETE | `/api/accounts/:id` | Archive account (soft delete) |

**GET /api/accounts – response:**
```json
[
  {
    "id": "...",
    "name": "Wallet",
    "type": "CASH",
    "currency": "HUF",
    "currentBalance": 45000.00,
    "color": "#4CAF50",
    "icon": "wallet",
    "isArchived": false
  }
]
```

### 7.3 Transactions

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/transactions` | List transactions (paginated, filtered) |
| POST | `/api/transactions` | Create transaction |
| GET | `/api/transactions/:id` | Get single transaction |
| PUT | `/api/transactions/:id` | Update transaction |
| DELETE | `/api/transactions/:id` | Delete transaction |
| GET | `/api/transactions/export` | Download CSV |

**GET /api/transactions – query params:**

| Param | Type | Description |
|-------|------|-------------|
| `page` | int | Page number (0-based) |
| `size` | int | Page size (default 50) |
| `from` | date | Start date (`YYYY-MM-DD`) |
| `to` | date | End date (`YYYY-MM-DD`) |
| `accountId` | UUID | Filter by account |
| `categoryId` | UUID | Filter by category |
| `type` | string | `EXPENSE`, `INCOME`, `TRANSFER` |
| `tagIds` | UUID[] | Filter by tags (comma-separated) |
| `search` | string | Full-text search on note |

**POST /api/transactions – request:**
```json
{
  "type": "EXPENSE",
  "amount": 3500.00,
  "currency": "HUF",
  "fromAccountId": "...",
  "categoryId": "...",
  "transactionDate": "2025-06-15T12:30:00Z",
  "note": "Lunch",
  "tagIds": ["...", "..."]
}
```

### 7.4 Categories

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/categories` | List all categories (tree structure) |
| POST | `/api/categories` | Create category |
| PUT | `/api/categories/:id` | Update category |
| DELETE | `/api/categories/:id` | Delete category |

### 7.5 Budgets

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/budgets` | List budgets with current-period progress |
| POST | `/api/budgets` | Create budget |
| PUT | `/api/budgets/:id` | Update budget |
| DELETE | `/api/budgets/:id` | Delete budget |

**GET /api/budgets – response (includes computed progress):**
```json
[
  {
    "id": "...",
    "categoryId": "...",
    "categoryName": "Food",
    "periodType": "MONTHLY",
    "amountLimit": 50000.00,
    "amountSpent": 23400.00,
    "currency": "HUF",
    "percentUsed": 46.8
  }
]
```

### 7.6 Tags

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/tags` | List all tags for current user |
| POST | `/api/tags` | Create tag |
| DELETE | `/api/tags/:id` | Delete tag |

### 7.7 Recurring Rules

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/recurring-rules` | List recurring rules |
| POST | `/api/recurring-rules` | Create rule |
| PUT | `/api/recurring-rules/:id` | Update rule |
| DELETE | `/api/recurring-rules/:id` | Delete rule |
| POST | `/api/recurring-rules/:id/toggle` | Activate / deactivate |

### 7.8 Reports

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/reports/dashboard` | Dashboard summary for current month |
| GET | `/api/reports/by-category` | Expenses/incomes grouped by category |
| GET | `/api/reports/income-vs-expense` | Monthly income vs expense (last N months) |
| GET | `/api/reports/account-balances` | All account balances |

All report endpoints accept `from` and `to` query params.

---

## 8. Frontend – Angular Application

### 8.1 Project Structure

```
src/
├── app/
│   ├── core/
│   │   ├── auth/
│   │   │   ├── auth.service.ts
│   │   │   ├── auth.guard.ts
│   │   │   └── jwt.interceptor.ts
│   │   ├── services/         (API service wrappers)
│   │   └── models/           (TypeScript interfaces mirroring backend DTOs)
│   ├── store/                (NgRx)
│   │   ├── auth/
│   │   ├── accounts/
│   │   ├── transactions/
│   │   ├── categories/
│   │   ├── budgets/
│   │   └── app.state.ts
│   ├── shared/
│   │   ├── components/       (reusable UI components)
│   │   └── pipes/
│   └── features/
│       ├── auth/             (login, register pages)
│       ├── dashboard/
│       ├── accounts/
│       ├── transactions/
│       ├── categories/
│       ├── budgets/
│       ├── recurring/
│       └── reports/
├── styles/
│   ├── _variables.scss       (design tokens)
│   ├── _mixins.scss
│   └── styles.scss           (global styles + PrimeNG theme overrides)
└── environments/
```

### 8.2 Routes

```
/                    → redirect to /dashboard (if authenticated) or /login
/login               → AuthLoginComponent
/register            → AuthRegisterComponent
/dashboard           → DashboardComponent
/accounts            → AccountListComponent
/accounts/:id        → AccountDetailComponent
/transactions        → TransactionListComponent
/transactions/new    → TransactionFormComponent
/transactions/:id    → TransactionFormComponent (edit mode)
/categories          → CategoryListComponent
/budgets             → BudgetListComponent
/recurring           → RecurringRuleListComponent
/reports             → ReportsComponent (tabbed)
```

All routes except `/login` and `/register` are guarded by `AuthGuard`.

### 8.3 NgRx State Design

Use NgRx when state is shared across multiple feature areas or needs to persist across route navigation.

**Slices:**

| Slice | Responsibility |
|-------|---------------|
| `auth` | Current user, tokens, login/logout actions |
| `accounts` | Account list, current balances, loading state |
| `transactions` | Paginated transaction list, active filters |
| `categories` | Category tree (used in transaction forms, reports, budgets) |
| `budgets` | Budget list with computed progress |

**Pattern per slice:**
- `state.ts` — state interface + initial state
- `actions.ts` — load, load success/failure, create, update, delete
- `effects.ts` — API calls triggered by actions
- `reducer.ts` — state transitions
- `selectors.ts` — memoized derived state

### 8.4 Key Components

**`TransactionFormComponent`**
- Used for both create and edit
- PrimeNG `p-dialog` or inline page
- Reactive form with `FormGroup`
- Account dropdown, category dropdown (filtered by type), date picker, amount input, tag multi-select, currency selector, note textarea

**`TransactionListComponent`**
- PrimeNG `p-table` with server-side pagination and lazy loading
- Filter panel: date range (`p-calendar`), account, category, type, tag, free-text search
- Row actions: edit, delete
- Summary row: total income, total expense for current filter

**`AccountCardComponent`**
- Displays account name, type icon, currency, and current balance
- Color-coded by account color

**`BudgetProgressComponent`**
- PrimeNG `p-progressBar` per budget
- Shows category, amount spent / limit, percentage
- Color changes (green → yellow → red) as usage increases

**`ReportsComponent`**
- Tabbed layout (`p-tabView`)
- Uses PrimeNG Chart (`p-chart`) wrapping Chart.js
  - Donut chart: expense by category
  - Bar chart: income vs expense by month
  - Line chart: account balance over time

**`RecurringRuleFormComponent`**
- Mirrors `TransactionFormComponent` but adds frequency, interval, start date, end date

### 8.5 JWT Interceptor

`jwt.interceptor.ts` attaches `Authorization: Bearer <token>` to every outgoing HTTP request. On receiving `401`, it:
1. Attempts a silent token refresh via `POST /api/auth/refresh`
2. On success, retries the original request with the new token
3. On failure, dispatches logout action and navigates to `/login`

### 8.6 Theming & Styling

- Use PrimeNG's `lara-light` or `aura` theme as a base; override via SCSS variables
- Global design tokens in `styles/_variables.scss`:
  - Primary color
  - Surface colors
  - Spacing scale
  - Border radius
- Component-specific styles in `.component.scss` files
- Utility classes for layout (flexbox/grid helpers)

---

## 9. Security Considerations

**Authentication**
- JWTs signed with a strong HS256 secret (min 256-bit key), rotated on deployment via environment variable
- Refresh tokens stored as SHA-256 hashes — raw token never persisted
- Access tokens are short-lived (15 min); refresh tokens expire in 30 days

**Authorization**
- Every backend query filters by `user_id` — no endpoint returns another user's data
- `@PreAuthorize("isAuthenticated()")` as a catch-all; ownership checks in service layer

**Input Validation**
- Jakarta Bean Validation (`@NotNull`, `@Positive`, `@Size`) on all request DTOs
- `GlobalExceptionHandler` converts `MethodArgumentNotValidException` to consistent 400 responses

**CORS**
- Configure allowed origins explicitly (e.g., `http://localhost:4200` for dev, production frontend URL for prod)
- Do not use `*` in production

**SQL Injection**
- All queries use JPA / parameterized native queries — no string concatenation in SQL

---

## 10. Development Workflow

1. **Scaffold backend** – Spring Initializr with Web, Security, JPA, PostgreSQL, Validation, Flyway, Lombok
2. **Set up PostgreSQL** – Docker Compose for local dev (`docker-compose.yml` with `postgres:16`)
3. **Flyway migrations** – Write initial `V1__init_schema.sql` with all tables and indexes
4. **Implement auth** – User registration, login, JWT filter, refresh token rotation
5. **Implement core entities** – Accounts, Categories (with seed data on registration), Tags
6. **Implement Transactions CRUD** – Including transfer logic and balance computation
7. **Implement Budgets & Recurring Rules** – Scheduler for recurring transaction generation
8. **Implement Report endpoints** – Aggregate SQL queries
9. **Scaffold Angular app** – `ng new` with routing and SCSS; install PrimeNG, NgRx
10. **Set up NgRx** – Auth slice first; add others incrementally
11. **Build core pages** – Auth pages, Dashboard, Transaction list + form
12. **Build remaining features** – Accounts, Categories, Budgets, Recurring, Reports
13. **Polish** – Loading states, error toasts (`p-toast`), empty states, mobile layout pass
14. **Docker Compose full stack** – Add Angular/nginx service alongside Spring Boot and PostgreSQL

### Environment Variables (Backend)

| Variable | Description |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | DB password |
| `JWT_SECRET` | HS256 signing key (min 32 chars) |
| `JWT_ACCESS_EXPIRY_MINUTES` | Access token TTL (default: 15) |
| `JWT_REFRESH_EXPIRY_DAYS` | Refresh token TTL (default: 30) |
| `ALLOWED_ORIGINS` | Comma-separated CORS origins |

### `docker-compose.yml` (Local Development)

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: expensetracker
      POSTGRES_USER: app
      POSTGRES_PASSWORD: secret
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  backend:
    build: ./backend
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/expensetracker
      SPRING_DATASOURCE_USERNAME: app
      SPRING_DATASOURCE_PASSWORD: secret
      JWT_SECRET: change-me-to-a-very-long-random-string
    ports:
      - "8080:8080"
    depends_on:
      - postgres

  frontend:
    build: ./frontend
    ports:
      - "4200:80"
    depends_on:
      - backend

volumes:
  pgdata:
```
