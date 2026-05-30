# Frontend Design System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Install PrimeNG + NgRx, set up the SCSS design token system, configure the Aura theme with the teal palette, and build the app shell with collapsible icon sidebar.

**Architecture:** PrimeNG Aura preset provides the component theme via CSS custom properties overridden by our teal palette. Global SCSS partials define all design tokens. The app shell is a standalone Angular component with a collapsible sidebar nav and a `<router-outlet>` for feature pages. Auth pages (`/login`, `/register`) bypass the shell via separate top-level routes.

**Tech Stack:** Angular 21, PrimeNG (Aura preset), NgRx 21, DM Sans (Google Fonts), SCSS

**Design spec:** `docs/superpowers/specs/2026-05-29-frontend-design.md`

---

## File Map

### New files
- `frontend/src/styles/_variables.scss` — SCSS tokens (colors, spacing, radii, shadows)
- `frontend/src/styles/_primeng.scss` — PrimeNG CSS custom property overrides
- `frontend/src/styles/_typography.scss` — DM Sans + type scale utility classes
- `frontend/src/styles/_layout.scss` — App shell + sidebar structural styles
- `frontend/src/styles/_components.scss` — Shared component styles (cards, badges, transaction rows, budget bars)
- `frontend/src/app/core/layout/shell/shell.component.ts`
- `frontend/src/app/core/layout/shell/shell.component.html`
- `frontend/src/app/core/layout/shell/shell.component.scss`
- `frontend/src/app/core/layout/shell/shell.component.spec.ts`
- `frontend/src/app/features/dashboard/dashboard.component.ts` (placeholder only)

### Modified files
- `frontend/src/styles.scss` — imports all partials
- `frontend/src/index.html` — DM Sans `<link>` preconnect tags
- `frontend/src/app/app.config.ts` — add `providePrimeNG` + Aura preset
- `frontend/src/app/app.routes.ts` — shell wrapper route + dashboard child

---

## Task 1: Commit design doc

**Files:**
- Already created: `docs/superpowers/specs/2026-05-29-frontend-design.md`

- [ ] **Step 1: Commit the design spec and .gitignore update**

```bash
git add docs/superpowers/specs/2026-05-29-frontend-design.md .gitignore
git commit -m "docs: add frontend design system spec"
```

---

## Task 2: Install packages

**Files:**
- Modify: `frontend/package.json` (via npm)

- [ ] **Step 1: Install PrimeNG, NgRx, and animations**

Run from `frontend/`:
```bash
npm install primeng @primeng/themes @ngrx/store @ngrx/effects @ngrx/store-devtools
```

Expected: packages added to `package.json` dependencies, no peer dependency errors.

> If `@primeng/themes` is not found, try `npm install primeng` only — themes may be bundled inside `primeng` for your version. Check the `node_modules/primeng/` directory structure or the PrimeNG changelog for Angular 21 compatibility.

- [ ] **Step 2: Verify install**

```bash
cat frontend/package.json | grep -E '"primeng|@ngrx'
```

Expected output (exact versions will vary):
```
"primeng": "^19.x.x",
"@ngrx/store": "^21.x.x",
"@ngrx/effects": "^21.x.x",
"@ngrx/store-devtools": "^21.x.x",
```

- [ ] **Step 3: Commit**

```bash
git add frontend/package.json frontend/package-lock.json
git commit -m "chore: install primeng, ngrx store/effects/devtools"
```

---

## Task 3: Set up SCSS token system

**Files:**
- Create: `frontend/src/styles/_variables.scss`
- Create: `frontend/src/styles/_primeng.scss`
- Create: `frontend/src/styles/_typography.scss`
- Create: `frontend/src/styles/_layout.scss`
- Create: `frontend/src/styles/_components.scss`
- Modify: `frontend/src/styles.scss`

- [ ] **Step 1: Create the styles directory**

```bash
mkdir -p frontend/src/styles
```

- [ ] **Step 2: Create `frontend/src/styles/_variables.scss`**

```scss
// Primary — Teal
$color-primary: #0d9488;
$color-primary-hover: #0f766e;
$color-primary-light: #14b8a6;
$color-primary-subtle: #ccfbf1;
$color-primary-faint: #f0fdfa;

// Surfaces & Backgrounds
$color-bg-page: #f8fafc;
$color-bg-card: #ffffff;
$color-bg-sidebar: #ffffff;
$color-border: #e2e8f0;
$color-border-strong: #cbd5e1;
$color-bg-muted: #f1f5f9;

// Text
$color-text-primary: #111827;
$color-text-body: #374151;
$color-text-secondary: #6b7280;
$color-text-muted: #9ca3af;

// Financial Semantic Colors
$color-income: #16a34a;
$color-income-light: #4ade80;
$color-income-faint: #bbf7d0;

$color-expense: #dc2626;
$color-expense-light: #f87171;
$color-expense-faint: #fee2e2;

$color-transfer: #2563eb;
$color-transfer-light: #93c5fd;
$color-transfer-faint: #dbeafe;

$color-budget-warn: #d97706;
$color-budget-warn-light: #fbbf24;
$color-budget-warn-faint: #fef3c7;

// Spacing (4px base unit)
$space-xs: 4px;
$space-sm: 8px;
$space-md: 12px;
$space-lg: 16px;
$space-xl: 24px;
$space-2xl: 32px;
$space-3xl: 48px;

// Border Radius
$radius-sm: 4px;
$radius-md: 8px;
$radius-lg: 12px;
$radius-xl: 16px;
$radius-pill: 9999px;

// Shadows
$shadow-xs: 0 1px 2px rgba(0, 0, 0, 0.06);
$shadow-sm: 0 1px 4px rgba(0, 0, 0, 0.08);
$shadow-md: 0 4px 12px rgba(0, 0, 0, 0.10);
$shadow-lg: 0 8px 24px rgba(0, 0, 0, 0.12);
```

- [ ] **Step 3: Create `frontend/src/styles/_primeng.scss`**

```scss
:root {
  --p-primary-color: #0d9488;
  --p-primary-hover-color: #0f766e;
  --p-primary-active-color: #0f766e;
  --p-primary-color-text: #ffffff;

  --p-surface-0: #ffffff;
  --p-surface-50: #f8fafc;
  --p-surface-100: #f1f5f9;
  --p-surface-200: #e2e8f0;
  --p-surface-300: #cbd5e1;
  --p-surface-400: #9ca3af;
  --p-surface-500: #6b7280;
  --p-surface-600: #374151;
  --p-surface-700: #111827;

  --p-border-radius-sm: 4px;
  --p-border-radius-md: 8px;
  --p-border-radius-lg: 12px;
  --p-border-radius-xl: 16px;

  --p-font-family: 'DM Sans', system-ui, sans-serif;

  --p-focus-ring-color: #ccfbf1;
  --p-focus-ring-shadow: 0 0 0 3px #ccfbf1;
}
```

> **Note:** Verify these token names against `node_modules/primeng/themes/aura/` — exact names vary by PrimeNG version. Adjust any that don't resolve.

- [ ] **Step 4: Create `frontend/src/styles/_typography.scss`**

```scss
// DM Sans is loaded via <link> in index.html

body {
  font-family: 'DM Sans', system-ui, sans-serif;
  font-size: 14px;
  color: $color-text-body;
  line-height: 1.5;
  -webkit-font-smoothing: antialiased;
}

h1,
h2,
h3,
h4,
h5,
h6 {
  color: $color-text-primary;
  line-height: 1.2;
  margin: 0;
}

.text-display {
  font-size: 32px;
  font-weight: 700;
  letter-spacing: -0.5px;
  color: $color-text-primary;
}

.text-page-title {
  font-size: 22px;
  font-weight: 700;
  color: $color-text-primary;
}

.text-section-heading {
  font-size: 16px;
  font-weight: 600;
  color: $color-text-primary;
}

.text-body {
  font-size: 14px;
  font-weight: 400;
  color: $color-text-body;
}

.text-secondary {
  font-size: 13px;
  font-weight: 400;
  color: $color-text-secondary;
}

.text-label {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.7px;
  color: $color-text-secondary;
}

.amount-income {
  color: $color-income;
  font-weight: 700;
}

.amount-expense {
  color: $color-expense;
  font-weight: 700;
}

.amount-transfer {
  color: $color-transfer;
  font-weight: 700;
}
```

- [ ] **Step 5: Create `frontend/src/styles/_layout.scss`**

```scss
*,
*::before,
*::after {
  box-sizing: border-box;
}

html,
body {
  height: 100%;
  margin: 0;
  padding: 0;
}

.app-shell {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: $color-bg-page;
}

.sidebar {
  width: 56px;
  flex-shrink: 0;
  background: $color-bg-sidebar;
  border-right: 1px solid $color-border;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 8px;
  gap: $space-xs;
  overflow: hidden;
  transition: width 0.2s ease;
  z-index: 10;

  &:hover {
    width: 200px;
    align-items: flex-start;
  }
}

.sidebar-logo {
  width: 32px;
  height: 32px;
  border-radius: $radius-md;
  background: $color-primary;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
  margin-bottom: $space-sm;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: $space-xs;
  width: 100%;
  flex: 1;
}

.sidebar-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 32px;
  border-radius: 7px;
  padding: 0 6px;
  cursor: pointer;
  text-decoration: none;
  overflow: hidden;
  white-space: nowrap;
  transition: background 0.15s ease;
  width: 36px;

  .sidebar:hover & {
    width: 184px;
  }

  &.active {
    background: $color-primary-subtle;

    .nav-icon {
      opacity: 1;
    }

    .nav-label {
      color: $color-primary;
      font-weight: 600;
    }
  }

  &:not(.active):hover {
    background: $color-bg-muted;
  }
}

.nav-icon {
  font-size: 16px;
  width: 20px;
  text-align: center;
  flex-shrink: 0;
  opacity: 0.4;

  .active & {
    opacity: 1;
  }
}

.nav-label {
  font-size: 13px;
  font-weight: 500;
  color: $color-text-secondary;
  opacity: 0;
  transition: opacity 0.15s ease;

  .sidebar:hover & {
    opacity: 1;
  }
}

.sidebar-footer {
  margin-top: auto;
  padding-top: $space-sm;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: $radius-pill;
  background: $color-bg-muted;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: $color-text-secondary;
  flex-shrink: 0;
}

.content-area {
  flex: 1;
  overflow-y: auto;
  padding: $space-xl;
}
```

- [ ] **Step 6: Create `frontend/src/styles/_components.scss`**

```scss
// Cards
.card {
  background: $color-bg-card;
  border-radius: $radius-lg;
  box-shadow: $shadow-sm;
  padding: $space-lg;
}

// Badges
.badge {
  display: inline-flex;
  align-items: center;
  padding: 3px $space-sm;
  border-radius: $radius-sm;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
}

.badge-income {
  background: $color-income-faint;
  color: $color-income;
}

.badge-expense {
  background: $color-expense-faint;
  color: $color-expense;
}

.badge-transfer {
  background: $color-transfer-faint;
  color: $color-transfer;
}

.badge-warn {
  background: $color-budget-warn-faint;
  color: $color-budget-warn;
}

.badge-neutral {
  background: $color-bg-muted;
  color: $color-text-secondary;
  font-weight: 500;
}

// Transaction list row
.transaction-row {
  display: flex;
  align-items: center;
  gap: $space-md;
  padding: $space-sm 0;
  border-bottom: 1px solid #f8fafc;

  &:last-child {
    border-bottom: none;
  }
}

.transaction-icon {
  width: 36px;
  height: 36px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.transaction-details {
  flex: 1;
  min-width: 0;

  .description {
    font-size: 14px;
    font-weight: 500;
    color: $color-text-primary;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .meta {
    font-size: 12px;
    color: $color-text-muted;
    margin-top: 1px;
  }
}

.transaction-amount {
  font-size: 15px;
  font-weight: 700;
  flex-shrink: 0;
}

// Budget progress bar
.budget-progress-bar {
  height: 6px;
  border-radius: $radius-sm;
  background: $color-bg-muted;
  overflow: hidden;

  .fill {
    height: 100%;
    border-radius: $radius-sm;
    background: $color-primary;
    transition: width 0.3s ease;

    &.warn {
      background: $color-budget-warn;
    }

    &.danger {
      background: $color-expense;
    }
  }
}
```

- [ ] **Step 7: Update `frontend/src/styles.scss`**

```scss
@use 'styles/variables' as *;
@use 'styles/primeng';
@use 'styles/typography';
@use 'styles/layout';
@use 'styles/components';
```

- [ ] **Step 8: Verify SCSS compiles**

```bash
cd frontend && ng build 2>&1 | head -30
```

Expected: build succeeds or fails only on missing PrimeNG import (fixed in Task 5). SCSS syntax errors would appear here — fix any before continuing.

- [ ] **Step 9: Commit**

```bash
git add frontend/src/styles/ frontend/src/styles.scss
git commit -m "feat: add SCSS design token system (variables, typography, layout, components)"
```

---

## Task 4: Load DM Sans font

**Files:**
- Modify: `frontend/src/index.html`

- [ ] **Step 1: Update `frontend/src/index.html`**

```html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <title>Expense Tracker</title>
    <base href="/" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <link rel="icon" type="image/x-icon" href="favicon.ico" />
    <link rel="preconnect" href="https://fonts.googleapis.com" />
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
    <link
      href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&display=swap"
      rel="stylesheet"
    />
  </head>
  <body>
    <app-root></app-root>
  </body>
</html>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/index.html
git commit -m "feat: load DM Sans font from Google Fonts"
```

---

## Task 5: Configure PrimeNG Aura theme

**Files:**
- Modify: `frontend/src/app/app.config.ts`

- [ ] **Step 1: Update `frontend/src/app/app.config.ts`**

```typescript
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeng/themes/aura';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: Aura,
        options: {
          prefix: 'p',
          darkModeSelector: 'none',
        },
      },
    }),
  ],
};
```

> **If the Aura import fails:** check `node_modules/primeng/themes/` — the import path may be `primeng/themes/aura` (no `@primeng/` scope) depending on the installed version.

- [ ] **Step 2: Verify build**

```bash
cd frontend && ng build 2>&1 | grep -E '^.*(error|Error)' | head -20
```

Expected: no TypeScript or module resolution errors. If there are import path errors for Aura, adjust the import as noted above and re-run.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/app.config.ts
git commit -m "feat: configure PrimeNG Aura theme with teal design tokens"
```

---

## Task 6: Create ShellComponent with sidebar

**Files:**
- Create: `frontend/src/app/core/layout/shell/shell.component.ts`
- Create: `frontend/src/app/core/layout/shell/shell.component.html`
- Create: `frontend/src/app/core/layout/shell/shell.component.scss`
- Create: `frontend/src/app/core/layout/shell/shell.component.spec.ts`

- [ ] **Step 1: Create the directory**

```bash
mkdir -p frontend/src/app/core/layout/shell
```

- [ ] **Step 2: Write the failing test**

Create `frontend/src/app/core/layout/shell/shell.component.spec.ts`:

```typescript
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { ShellComponent } from './shell.component';

describe('ShellComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('renders the sidebar', () => {
    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('.sidebar'))).toBeTruthy();
  });

  it('renders a router-outlet', () => {
    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('router-outlet'))).toBeTruthy();
  });

  it('renders nav items for all 7 main routes', () => {
    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    const items = fixture.debugElement.queryAll(By.css('.sidebar-nav-item'));
    expect(items.length).toBe(7);
  });

  it('renders the logo mark', () => {
    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('.sidebar-logo'))).toBeTruthy();
  });

  it('renders the user avatar', () => {
    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('.user-avatar'))).toBeTruthy();
  });
});
```

- [ ] **Step 3: Run the test to confirm it fails**

```bash
cd frontend && ng test --testPathPattern=shell.component.spec 2>&1 | tail -20
```

Expected: FAIL — `Cannot find module './shell.component'`.

- [ ] **Step 4: Create `shell.component.ts`**

```typescript
import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

interface NavItem {
  path: string;
  icon: string;
  label: string;
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  readonly navItems: NavItem[] = [
    { path: '/dashboard', icon: '⊞', label: 'Dashboard' },
    { path: '/accounts', icon: '🏦', label: 'Accounts' },
    { path: '/transactions', icon: '↕', label: 'Transactions' },
    { path: '/categories', icon: '🏷', label: 'Categories' },
    { path: '/budgets', icon: '◎', label: 'Budgets' },
    { path: '/reports', icon: '📊', label: 'Reports' },
    { path: '/recurring', icon: '🔁', label: 'Recurring' },
  ];
}
```

- [ ] **Step 5: Create `shell.component.html`**

```html
<div class="app-shell">
  <nav class="sidebar">
    <div class="sidebar-logo">E</div>

    <div class="sidebar-nav">
      @for (item of navItems; track item.path) {
        <a class="sidebar-nav-item" [routerLink]="item.path" routerLinkActive="active">
          <span class="nav-icon">{{ item.icon }}</span>
          <span class="nav-label">{{ item.label }}</span>
        </a>
      }
    </div>

    <div class="sidebar-footer">
      <div class="user-avatar">M</div>
    </div>
  </nav>

  <main class="content-area">
    <router-outlet />
  </main>
</div>
```

- [ ] **Step 6: Create `shell.component.scss`**

```scss
// Layout classes come from the global _layout.scss.
// :host sets the component to fill its parent's height.
:host {
  display: block;
  height: 100%;
}
```

- [ ] **Step 7: Run tests to confirm they pass**

```bash
cd frontend && ng test --testPathPattern=shell.component.spec 2>&1 | tail -20
```

Expected: 5 tests PASS.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/app/core/
git commit -m "feat: add ShellComponent with collapsible icon sidebar"
```

---

## Task 7: Wire routes and verify in browser

**Files:**
- Modify: `frontend/src/app/app.routes.ts`
- Create: `frontend/src/app/features/dashboard/dashboard.component.ts`

- [ ] **Step 1: Create `frontend/src/app/features/dashboard/dashboard.component.ts`**

```typescript
import { Component } from '@angular/core';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  template: `
    <h1 class="text-page-title">Dashboard</h1>
    <p class="text-secondary">Welcome to Expense Tracker.</p>
  `,
})
export class DashboardComponent {}
```

- [ ] **Step 2: Update `frontend/src/app/app.routes.ts`**

```typescript
import { Routes } from '@angular/router';
import { ShellComponent } from './core/layout/shell/shell.component';

export const routes: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(
            (m) => m.DashboardComponent,
          ),
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },
];
```

> Additional feature routes (`/accounts`, `/transactions`, etc.) are added here as each feature module is scaffolded. Auth routes (`/login`, `/register`) will be added as top-level routes without the shell.

- [ ] **Step 3: Start the dev server**

```bash
cd frontend && ng serve
```

- [ ] **Step 4: Verify in the browser at http://localhost:4200**

Check all of the following:
- White sidebar (56px) on the left with teal E logo mark and 7 faint nav icons
- Light gray (`#f8fafc`) content area on the right showing "Dashboard" heading in DM Sans
- Hovering the sidebar smoothly expands it to 200px showing nav labels
- Dashboard nav item has a teal-100 background (active state)
- No console errors in DevTools

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/app.routes.ts frontend/src/app/features/
git commit -m "feat: wire ShellComponent into app routes with dashboard placeholder"
```
