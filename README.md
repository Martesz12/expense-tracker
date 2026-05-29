# Expense Tracker

A personal finance web application for tracking accounts, transactions, budgets, and spending patterns. Think of it as a self-hosted, simplified version of apps like 1Money.

## My Experiment with Agentic Development

This is my first project where I used a coding agent — [Claude Code](https://claude.ai/code) — as a primary development tool. Rather than just using it for occasional help, I wanted to see how far I could take it: writing features end-to-end, reviewing code for security and correctness, scaffolding boilerplate, enforcing conventions automatically, and managing the dev environment.

The project is as much an experiment in agentic development as it is a real application. I've been building up a custom tooling layer around Claude Code — skills, subagents, hooks, and MCP integrations — to see what a well-configured AI coding workflow actually looks like in practice.

## Features

- JWT authentication with refresh token rotation
- Multiple account types (cash, bank, credit card, savings, investment)
- Transaction management with multi-currency support
- Custom categories with icons and colors
- Budget tracking with progress visualization
- Recurring transactions with automated scheduling
- Reports and spending statistics
- Tags and CSV export

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Angular 21, NgRx, PrimeNG |
| Backend | Spring Boot 3.x, Java 21 |
| Database | PostgreSQL 16, Flyway migrations |
| Infrastructure | Docker, Docker Compose |

## Agentic Tooling

Here's a breakdown of the Claude Code tooling I've set up and used throughout this project.

### Plugins

Official Claude Code plugins that extend its capabilities with skills and subagents:

| Plugin | Purpose |
|--------|---------|
| `superpowers` | Advanced development workflows: brainstorming, planning, systematic debugging, code review, finishing branches |
| `frontend-design` | Building distinctive, production-grade UI components |
| `context7` | Live documentation lookup for any library or framework |
| `playwright` | Browser automation for UI verification |
| `code-simplifier` | Reviewing and simplifying recently changed code |
| `claude-md-management` | Auditing and keeping CLAUDE.md files up to date |
| `security-guidance` | Security review guidance and best practices |
| `skill-creator` | Creating new custom skills |
| `claude-code-setup` | Recommending Claude Code automations for a codebase |

### Custom Project Skills

Skills I built specifically for this project to automate repetitive scaffolding:

| Skill | What it does |
|-------|-------------|
| `create-migration` | Scaffolds the next Flyway migration file with the correct version number and naming convention |
| `new-feature` | Scaffolds a complete backend feature package: entity, repository, service, controller, and DTOs |
| `new-feature-module` | Scaffolds a lazy-loaded Angular feature module with routes, list component, detail component, and NgRx store stub |
| `new-ngrx-slice` | Scaffolds a full NgRx feature slice: actions, reducer, effects, selectors, and model |
| `start-dev` | Starts the local Docker dev environment, prompting whether to bring up only PostgreSQL or the full stack |

### Custom Subagents

Specialized agents invoked automatically during development for domain-specific review:

| Subagent | What it checks |
|----------|---------------|
| `ngrx-reviewer` | Reviews NgRx code for anti-patterns: manual subscriptions instead of `async` pipe, state mutations in effects, missing `createSelector` memoization, improper action naming |
| `security-reviewer` | Reviews auth and transaction code for cross-user data leakage, JWT validation gaps, missing userId scoping, SQL injection via JPQL, and sensitive data in logs |

### MCP Servers

Model Context Protocol servers that give Claude Code direct access to external systems:

| MCP Server | Purpose |
|------------|---------|
| `postgres` | Read-only SQL access to the local dev database for querying data mid-task |
| `context7` | Powers the context7 plugin — fetches up-to-date library and framework documentation |
| `playwright` | Browser automation — navigating, clicking, screenshotting the running app for UI verification |

### Automated Hooks

Hooks that run automatically on every file edit, without me having to ask:

- **Prettier** — formats any modified frontend file on save
- **TypeScript check** — runs `tsc --noEmit` on every `.ts` file change
- **ESLint** — runs `ng lint` on every `.ts` and `.html` change
- **Sensitive file guard** — blocks accidental edits to `.env` and `package-lock.json`
