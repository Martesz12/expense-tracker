---
name: new-feature-module
description: Scaffold a new lazy-loaded Angular feature module under frontend/src/app/features/{name}/. Creates the routes file, a list component, a detail component, and a store/ subfolder stub. Wires it into the root router.
---

Ask the user for the feature name (e.g. "budgets"). Then:

1. Create `frontend/src/app/features/{name}/` with:
   - `{name}.routes.ts` — standalone routes array, lazy-loaded
   - `{name}-list/{name}-list.ts` — standalone list component stub
   - `{name}-detail/{name}-detail.ts` — standalone detail/form component stub
   - `store/` — empty folder (tell the user to run /new-ngrx-slice next)

2. Add a lazy route entry to `frontend/src/app/app.routes.ts`:
   `{ path: '{name}', loadChildren: () => import('./features/{name}/{name}.routes').then(m => m.{NAME}_ROUTES) }`

3. Confirm what was created and remind the user to run /new-ngrx-slice to add state.
