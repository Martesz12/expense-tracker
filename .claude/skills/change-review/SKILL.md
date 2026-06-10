---
name: change-review
description: Reviews local code changes (git diff) for code smells, clean code principle violations, and logic flaws, then provides concrete fix suggestions. Invoke when the user says "review my changes", "check my code", "any code smells?", "do a self-review", "jolly raccoon", "review the diff", "check for issues", "review what I wrote", or similar. Also trigger after completing a feature or bugfix when the user wants a quality check before committing. This is a developer self-review tool — use it proactively whenever it sounds like the user wants a second pair of eyes on their local work before pushing.
---

# Change Review

Review local code changes for quality issues and suggest concrete fixes.

## Step 1 — Get the diff

Run `git diff HEAD` to capture all local changes (staged and unstaged).

- If that returns nothing, fall back to `git diff HEAD~1` to review the last commit.
- If still nothing, tell the user there are no changes to review.

## Step 2 — Gather full context

For each file touched in the diff, read the **full file** — not just the changed lines. Issues like null dereferences, accidental duplication of an existing utility, or SRP violations are often only visible in context.

## Step 3 — Analyze across three categories

Check every changed file for all three categories. Don't stop at the first issue per file.

### Code Smells
- **Long method**: a function over ~20 lines that is trying to do too much
- **God class / large file**: a class that owns too many responsibilities
- **Duplicate logic**: code that repeats something already expressed elsewhere in the same file or a sibling file
- **Magic numbers/strings**: bare literals (e.g. `86400`, `"ADMIN"`) with no named constant
- **Deep nesting**: more than 3 levels of `if`/`for`/`try` nesting — consider early returns or extraction
- **Dead code**: unreachable branches, commented-out blocks, unused parameters
- **Overly long parameter list**: more than 3–4 parameters usually signals missing abstraction

### Clean Code
- **Poor naming**: abbreviations, misleading names, single-letter variables outside loop counters (e.g. `t`, `tmp`, `data`)
- **SRP violation**: a function whose name says one thing but whose body does two or three (e.g. `validateAndSave()`)
- **What-comments**: comments that explain *what* code does instead of *why* it does it (the code should explain what; comments explain non-obvious reasons)
- **Complex boolean**: a multi-term condition (`a && !b || c.isX()`) that would be clearer as a named boolean or extracted predicate
- **Unnecessary code**: defensive checks for conditions that can't happen, over-engineering for a single use case

### Logic Flaws
- **Null/undefined dereference risk**: calling a method or accessing a field on something that could be null without a guard
- **Boundary conditions**: loops or slices where the start/end index is off by one
- **Incorrect negation**: `!= null` where `== null` was intended, inverted boolean logic
- **Swallowed exceptions**: `catch (e) {}` or `catch (e) { log(e); }` with no recovery or re-throw when the caller needs to know
- **Missing edge cases**: a new method that handles the happy path but ignores empty collections, zero values, or the null case
- **Race condition**: shared mutable state modified without synchronization (in multi-threaded or async code)

## Step 4 — Output the report

Use this exact structure. Omit a section entirely if there are zero findings in it.

```
## Change Review

N critical · N warnings · N suggestions

### Critical
[Logic] `src/service/OrderService.java:42` — Null pointer risk: `order.getCustomer().getEmail()` called without null check on `getCustomer()`
Suggestion: Guard with `if (order.getCustomer() != null)` before dereferencing, or change `getCustomer()` to return `Optional<Customer>`.

### Warnings
[Code Smell] `src/service/OrderService.java:88-112` — `processOrder()` is 24 lines and does three distinct things: validates the order, persists it, and sends a confirmation email
Suggestion: Extract into `validateOrder()`, `saveOrder()`, and `sendConfirmation()`. Each should be short enough to fit on a screen.

### Suggestions
[Clean Code] `src/util/DateUtil.java:17` — Variable `t` has no clear meaning
Suggestion: Rename to `targetDate` (or whatever the intent is) — single-letter names slow readers down.
```

### Severity guide
- **Critical** — will likely cause a runtime bug, data corruption, or silent failure in production
- **Warning** — violates a design principle; will cause maintainability pain as the codebase grows
- **Suggestion** — minor improvement to naming, readability, or style

### When there are no issues
Say so clearly:

```
## Change Review

No issues found — changes look clean.
```

Don't invent issues. If the code is good, say so.

## Scope

This skill reviews code quality only — not security vulnerabilities (use `security-review` for that) and not PR compliance (use `code-review` for that).
