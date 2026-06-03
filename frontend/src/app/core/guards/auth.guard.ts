import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { filter, first, map, switchMap } from 'rxjs';

import { selectInitialized } from '../../features/auth/store/auth.reducer';
import { selectIsAuthenticated } from '../../features/auth/store/auth.selectors';

export const authGuard: CanActivateFn = (_, state) => {
  const store = inject(Store);
  const router = inject(Router);

  return store.select(selectInitialized).pipe(
    filter((initialized) => initialized),
    first(),
    switchMap(() => store.select(selectIsAuthenticated).pipe(first())),
    map(
      (isAuthenticated) =>
        isAuthenticated ||
        router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } }),
    ),
  );
};
