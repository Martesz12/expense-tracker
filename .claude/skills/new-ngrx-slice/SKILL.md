---
name: new-ngrx-slice
description: Scaffold a complete NgRx feature slice (actions, reducer, effects, selectors, and model) under frontend/src/app/features/{name}/store/. Follows the project's NgRx conventions. Use when adding a new state slice.
---

Ask the user for the slice name (e.g. "accounts"). Then create the following files
under `frontend/src/app/features/{name}/store/`:

- `{name}.actions.ts` — load/loadSuccess/loadFailure, create/createSuccess/createFailure,
  update/updateSuccess/updateFailure, delete/deleteSuccess/deleteFailure
- `{name}.reducer.ts` — state interface with `items`, `selectedId`, `loading`, `error`;
  use `createFeature` and `createReducer` with `on()`
- `{name}.effects.ts` — one effect per mutation action; inject the feature's API service;
  use `createEffect`, `Actions`, `ofType`, `switchMap`/`exhaustMap`, `catchError`
- `{name}.selectors.ts` — `selectAll`, `selectSelected`, `selectLoading`, `selectError`
  derived from the feature selector
- `{name}.model.ts` — TypeScript interface matching the backend DTO from SPEC.md

Register the feature in the slice's feature module `provideState()` and `provideEffects()`.

Follow the pattern in any existing slice under `frontend/src/app/features/`.
