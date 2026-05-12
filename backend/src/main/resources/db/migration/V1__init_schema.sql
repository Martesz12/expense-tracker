-- ============================================================
-- V1__init_schema.sql
-- Full initial schema for Expense Tracker
-- ============================================================

-- users ----------------------------------------------------
CREATE TABLE users (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name          VARCHAR(100)  NOT NULL,
  email         VARCHAR(255)  NOT NULL UNIQUE,
  password      VARCHAR(255)  NOT NULL,
  home_currency VARCHAR(3)    NOT NULL DEFAULT 'EUR',
  created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- refresh_tokens -------------------------------------------
CREATE TABLE refresh_tokens (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash  VARCHAR(255)  NOT NULL UNIQUE,
  expires_at  TIMESTAMPTZ   NOT NULL,
  revoked     BOOLEAN       NOT NULL DEFAULT FALSE,
  created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- accounts -------------------------------------------------
CREATE TABLE accounts (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name            VARCHAR(100)  NOT NULL,
  type            VARCHAR(20)   NOT NULL,
  currency        VARCHAR(3)    NOT NULL,
  initial_balance NUMERIC(15,2) NOT NULL DEFAULT 0,
  color           VARCHAR(7),
  icon            VARCHAR(50),
  is_archived     BOOLEAN       NOT NULL DEFAULT FALSE,
  created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- categories -----------------------------------------------
CREATE TABLE categories (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name        VARCHAR(100)  NOT NULL,
  type        VARCHAR(10)   NOT NULL,
  icon        VARCHAR(50),
  color       VARCHAR(7),
  parent_id   UUID REFERENCES categories(id) ON DELETE SET NULL,
  created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- tags -----------------------------------------------------
CREATE TABLE tags (
  id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name    VARCHAR(50)  NOT NULL,
  UNIQUE (user_id, name)
);

-- recurring_rules ------------------------------------------
CREATE TABLE recurring_rules (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  description     VARCHAR(255),
  type            VARCHAR(10)   NOT NULL,
  amount          NUMERIC(15,2) NOT NULL,
  currency        VARCHAR(3)    NOT NULL,
  from_account_id UUID          NOT NULL REFERENCES accounts(id),
  to_account_id   UUID          REFERENCES accounts(id),
  category_id     UUID          REFERENCES categories(id),
  note            TEXT,
  frequency       VARCHAR(10)   NOT NULL,
  interval_value  INTEGER       NOT NULL DEFAULT 1,
  start_date      DATE          NOT NULL,
  end_date        DATE,
  next_occurrence DATE          NOT NULL,
  is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
  created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- transactions ---------------------------------------------
CREATE TABLE transactions (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id           UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type              VARCHAR(10)   NOT NULL,
  amount            NUMERIC(15,2) NOT NULL,
  currency          VARCHAR(3)    NOT NULL,
  exchange_rate     NUMERIC(15,6),
  from_account_id   UUID          NOT NULL REFERENCES accounts(id),
  to_account_id     UUID          REFERENCES accounts(id),
  category_id       UUID          REFERENCES categories(id),
  note              TEXT,
  transaction_date  TIMESTAMPTZ   NOT NULL,
  recurring_rule_id UUID          REFERENCES recurring_rules(id) ON DELETE SET NULL,
  created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- transaction_tags -----------------------------------------
CREATE TABLE transaction_tags (
  transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
  tag_id         UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
  PRIMARY KEY (transaction_id, tag_id)
);

-- budgets --------------------------------------------------
CREATE TABLE budgets (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  category_id  UUID          NOT NULL REFERENCES categories(id),
  period_type  VARCHAR(10)   NOT NULL,
  period_start DATE,
  period_end   DATE,
  amount_limit NUMERIC(15,2) NOT NULL,
  currency     VARCHAR(3)    NOT NULL,
  created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- ============================================================
-- Indexes
-- ============================================================

CREATE INDEX idx_transactions_user_id
  ON transactions(user_id);

CREATE INDEX idx_transactions_date
  ON transactions(user_id, transaction_date DESC);

CREATE INDEX idx_transactions_from_account
  ON transactions(from_account_id);

CREATE INDEX idx_transactions_to_account
  ON transactions(to_account_id);

CREATE INDEX idx_transactions_category
  ON transactions(category_id);

CREATE INDEX idx_accounts_user_id
  ON accounts(user_id);

CREATE INDEX idx_categories_user_id
  ON categories(user_id);

CREATE INDEX idx_budgets_user_id
  ON budgets(user_id);

CREATE INDEX idx_refresh_tokens_user_id
  ON refresh_tokens(user_id);

CREATE INDEX idx_recurring_rules_next_occurrence
  ON recurring_rules(next_occurrence)
  WHERE is_active = TRUE;
