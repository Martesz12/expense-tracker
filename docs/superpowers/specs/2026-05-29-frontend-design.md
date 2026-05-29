# Frontend Design System

**Date:** 2026-05-29  
**Status:** Approved  
**Scope:** Visual design tokens, component styles, and layout structure for the Angular 21 + PrimeNG frontend.

---

## Context

The frontend is a greenfield Angular 21 app. Before scaffolding any feature modules, we need a stable design system to ensure visual consistency across all pages. The goal is a **clean, modern app aesthetic** — not a banking dashboard, not a consumer toy. Restrained, functional, pleasant to use daily.

**Key decisions made:**
- Direction: Modern App (rounded cards, subtle shadows, one accent color)
- Accent color: Teal
- Navigation: Collapsible icon sidebar (expands on hover)
- Font: DM Sans
- Theme mode: Light only
- Implementation approach: PrimeNG Aura theme + CSS custom property overrides

---

## Implementation Approach

Use **PrimeNG's Aura preset** as the base theme. Aura is built on CSS custom properties (design tokens), so overriding `--p-primary-*`, surface colors, border radii, and font family propagates automatically to all PrimeNG components. Custom SCSS lives in `frontend/src/styles/`.

File structure:
```
frontend/src/styles/
  _variables.scss     ← design tokens (colors, spacing, radii, shadows)
  _primeng.scss       ← PrimeNG token overrides mapped to our palette
  _typography.scss    ← DM Sans import + type scale utilities
  _layout.scss        ← sidebar + page shell styles
  _components.scss    ← small component overrides (transaction rows, badges, etc.)
```

`styles.scss` imports all of the above in order.

---

## Color System

### Primary — Teal

| Token | Value | Usage |
|-------|-------|-------|
| `$color-primary` | `#0d9488` | Buttons, active nav, links |
| `$color-primary-hover` | `#0f766e` | Button hover state |
| `$color-primary-light` | `#14b8a6` | Secondary accents |
| `$color-primary-subtle` | `#ccfbf1` | Active nav background, badge fill |
| `$color-primary-faint` | `#f0fdfa` | Hover row tint, input focus ring bg |

### Surfaces & Backgrounds

| Token | Value | Usage |
|-------|-------|-------|
| `$color-bg-page` | `#f8fafc` | Page/app background |
| `$color-bg-card` | `#ffffff` | Cards, modals, dropdowns |
| `$color-bg-sidebar` | `#ffffff` | Sidebar background |
| `$color-border` | `#e2e8f0` | Default borders, dividers |
| `$color-border-strong` | `#cbd5e1` | Strong dividers, table column borders |
| `$color-bg-muted` | `#f1f5f9` | Inactive nav items, disabled inputs, table alternating rows |

### Text

| Token | Value | Usage |
|-------|-------|-------|
| `$color-text-primary` | `#111827` | Headings, amounts, primary labels |
| `$color-text-body` | `#374151` | Body text, form values |
| `$color-text-secondary` | `#6b7280` | Dates, subcategories, secondary labels |
| `$color-text-muted` | `#9ca3af` | Placeholder text, helper text |

### Semantic — Financial Data

| State | Primary | Light | Faint |
|-------|---------|-------|-------|
| Income | `#16a34a` | `#4ade80` | `#bbf7d0` |
| Expense | `#dc2626` | `#f87171` | `#fee2e2` |
| Transfer | `#2563eb` | `#93c5fd` | `#dbeafe` |
| Budget warning | `#d97706` | `#fbbf24` | `#fef3c7` |

Income amounts are displayed in `#16a34a`, expense amounts in `#dc2626`, transfers in `#2563eb`. Category icon backgrounds use the faint color; the icon itself uses the primary semantic color.

---

## Typography

**Font:** DM Sans (Google Fonts) — loaded via `@import` in `_typography.scss`.

| Role | Size | Weight | Notes |
|------|------|--------|-------|
| Display (hero numbers) | 32px | 700 | Letter-spacing −0.5px. Dashboard balance, account totals. |
| Page title | 22px | 700 | Top of each page |
| Section heading | 16px | 600 | Card titles, modal headings |
| Body | 14px | 400 | Default text, transaction descriptions, form values |
| Secondary | 13px | 400 | Dates, account names, supporting detail. Color: `$color-text-secondary` |
| Label / Caption | 11px | 600 | Uppercase, letter-spacing 0.7px. Stat card labels, column headers |

Line height: 1.5 for body text, 1.2 for headings.

---

## Spacing Scale

Base unit: **4px**.

| Token | Value | Usage |
|-------|-------|-------|
| `$space-xs` | 4px | Icon gaps, tight padding |
| `$space-sm` | 8px | Badge padding, compact gaps |
| `$space-md` | 12px | Input padding, list item gaps |
| `$space-lg` | 16px | Card padding, section gaps |
| `$space-xl` | 24px | Page header margin, between-card gaps |
| `$space-2xl` | 32px | Section top margins |
| `$space-3xl` | 48px | Page-level vertical padding |

---

## Border Radius

| Token | Value | Usage |
|-------|-------|-------|
| `$radius-sm` | 4px | Badges, tags, small pills |
| `$radius-md` | 8px | Inputs, buttons, table rows |
| `$radius-lg` | 12px | Cards, panels |
| `$radius-xl` | 16px | Modals, drawers |
| `$radius-pill` | 9999px | Round icon buttons, pill badges |

---

## Shadow Scale

| Token | Value | Usage |
|-------|-------|-------|
| `$shadow-xs` | `0 1px 2px rgba(0,0,0,0.06)` | Table rows with border |
| `$shadow-sm` | `0 1px 4px rgba(0,0,0,0.08)` | Cards (default) |
| `$shadow-md` | `0 4px 12px rgba(0,0,0,0.10)` | Dropdowns, popovers |
| `$shadow-lg` | `0 8px 24px rgba(0,0,0,0.12)` | Modals |

---

## Layout — Icon Sidebar

The app shell is a two-column flex layout: a fixed sidebar on the left, a scrollable content area on the right.

**Sidebar:**
- **Collapsed (default):** 56px wide. Shows logo mark + icon-only nav items.
- **Expanded (on hover):** 200px wide, slides open with a CSS transition. Shows icons + labels.
- Background: `#ffffff`, right border: `1px solid $color-border`.
- Active nav item: background `$color-primary-subtle`, icon + label in `$color-primary`, font-weight 600.
- Inactive nav items: icon opacity 0.4, label color `$color-text-secondary`.
- Bottom of sidebar: user avatar (circle with initials).

**Navigation items (in order):** Dashboard, Accounts, Transactions, Categories, Budgets, Reports, Recurring. Settings at the bottom.

**Content area:**
- Background: `$color-bg-page` (`#f8fafc`).
- Padding: `$space-xl` (24px) on all sides.
- Max content width: none — fills available space.

---

## Core Component Styles

### Buttons

| Variant | Background | Text | Border |
|---------|-----------|------|--------|
| Primary | `$color-primary` | white | none |
| Outlined | `$color-primary-faint` | `$color-primary` | 1.5px `$color-primary` |
| Secondary | `$color-bg-muted` | `$color-text-body` | none |
| Danger | transparent | `#dc2626` | 1.5px `#fca5a5` |
| Disabled | `$color-bg-muted` | `$color-text-muted` | none |

All buttons: `$radius-md` (8px), padding `9px 18px`, font-size 14px, font-weight 600.

### Form Inputs

- Border: `1.5px solid $color-border`
- Border (focused): `1.5px solid $color-primary`
- Border (error): `1.5px solid #fca5a5`
- Border-radius: `$radius-md` (8px)
- Padding: `9px 12px`
- Background: white
- Font-size: 14px, color `$color-text-body`
- Placeholder color: `$color-text-muted`
- Error message: 11px, color `#dc2626`, appears below the input

### Cards

- Background: white
- Border-radius: `$radius-lg` (12px)
- Box-shadow: `$shadow-sm`
- Padding: `$space-lg` (16px)
- No visible border (shadow provides depth)

### Transaction List Rows

- Layout: flex row with 36×36px category icon, description block, and right-aligned amount
- Category icon: `$radius-md` (8px) container, background uses semantic faint color, emoji/icon centered
- Description: primary line 14px/500, secondary line (date + category) 12px `$color-text-muted`
- Amount: 15px font-weight 700, color matches transaction type (income/expense/transfer)
- Row separator: `1px solid #f1f5f9` — used in dashboard recent-transactions list
- Alternating rows: even rows get `$color-bg-muted` background — used in the full `/transactions` data table instead of separators

### Badges & Tags

- Category badges: `$radius-sm` (4px), 11px, weight 600, colored background (semantic faint) + colored text (semantic primary)
- User tags: `$radius-sm`, 11px, weight 500, background `$color-bg-muted`, text `$color-text-secondary`
- Budget warning badge: amber palette (`#fef3c7` bg, `#d97706` text)

### Dashboard Stat Cards

- Four equal-width cards in a CSS grid row
- Each card: white, `$radius-lg`, `$shadow-sm`, padding `$space-lg`
- Structure: uppercase label (11px/600) → large number (22px/700) → trend/subtext (11px)
- Total balance: text `$color-text-primary`
- Income number: `#16a34a`
- Expense number: `#dc2626`
- Teal metric (savings rate, etc.): `$color-primary`

---

## PrimeNG Aura Token Mapping

These CSS custom property overrides go in `_primeng.scss` and map our tokens onto PrimeNG's Aura preset:

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

---

## Pages Summary

| Route | Key components |
|-------|---------------|
| `/dashboard` | Stat cards grid, expense category bars, recent transactions list |
| `/accounts` | Account card list (balance, type, currency), add account button |
| `/transactions` | Filterable PrimeNG data table, inline amount color coding, tag chips |
| `/categories` | Category grid cards with color + icon, create/edit modal |
| `/budgets` | Budget cards with progress bars (teal → amber → red as % increases) |
| `/reports` | Tab navigation, chart panels (Chart.js via PrimeNG p-chart) |
| `/recurring` | Rule cards with frequency badge, next-run date |
| `/login`, `/register` | Centered card layout, no sidebar |

---

## What This Spec Does Not Cover

- Responsive / mobile layout (desktop-first per SPEC, tablet-usable but not a priority now)
- Dark mode (deferred)
- Animation/transition specifics beyond sidebar expand
- Individual form layout details (handled per-feature when scaffolded)
