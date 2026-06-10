import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Actions, createEffect, ofType, ROOT_EFFECTS_INIT } from '@ngrx/effects';
import { catchError, exhaustMap, map, of, tap } from 'rxjs';

import { AuthActions } from './auth.actions';
import { AuthService } from '../../../core/services/auth.service';
import { ApiError } from '../../../core/models/auth/api-error.model';

function extractErrorMessage(err: unknown): string {
  const body = (err as HttpErrorResponse)?.error as ApiError | null;
  return body?.message ?? 'An unexpected error occurred. Please try again.';
}

@Injectable()
export class AuthEffects {
  private readonly actions$ = inject(Actions);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  initSession$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(ROOT_EFFECTS_INIT),
      map(() => AuthActions.refreshToken()),
    );
  });

  login$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(AuthActions.login),
      exhaustMap(({ request, returnUrl }) =>
        this.authService.login(request).pipe(
          map((response) => AuthActions.loginSuccess({ response, returnUrl })),
          catchError((err: unknown) =>
            of(AuthActions.loginFailure({ error: extractErrorMessage(err) })),
          ),
        ),
      ),
    );
  });

  register$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(AuthActions.register),
      exhaustMap(({ request }) =>
        this.authService.register(request).pipe(
          map((response) => AuthActions.registerSuccess({ response })),
          catchError((err: unknown) =>
            of(AuthActions.registerFailure({ error: extractErrorMessage(err) })),
          ),
        ),
      ),
    );
  });

  loginSuccess$ = createEffect(
    () => {
      return this.actions$.pipe(
        ofType(AuthActions.loginSuccess),
        tap(({ returnUrl }) => {
          const destination = returnUrl && returnUrl.startsWith('/') ? returnUrl : '/dashboard';
          this.router.navigate([destination]);
        }),
      );
    },
    { dispatch: false },
  );

  registerSuccess$ = createEffect(
    () => {
      return this.actions$.pipe(
        ofType(AuthActions.registerSuccess),
        tap(() => {
          this.router.navigate(['/dashboard']);
        }),
      );
    },
    { dispatch: false },
  );

  logout$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(AuthActions.logout),
      exhaustMap(() =>
        this.authService.logout().pipe(
          map(() => AuthActions.logoutSuccess()),
          catchError(() => of(AuthActions.logoutSuccess())),
        ),
      ),
    );
  });

  logoutOrRefreshFailure$ = createEffect(
    () => {
      return this.actions$.pipe(
        ofType(AuthActions.logoutSuccess, AuthActions.refreshTokenFailure),
        tap(({ type }) => {
          const isRefreshFailure = type === AuthActions.refreshTokenFailure.type;
          const currentUrl = this.router.url;
          const queryParams =
            isRefreshFailure && !currentUrl.startsWith('/login')
              ? { returnUrl: currentUrl }
              : undefined;
          this.router.navigate(['/login'], queryParams ? { queryParams } : undefined);
        }),
      );
    },
    { dispatch: false },
  );

  refreshToken$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(AuthActions.refreshToken),
      exhaustMap(() =>
        this.authService.refresh().pipe(
          map((response) => AuthActions.refreshTokenSuccess({ response })),
          catchError(() => of(AuthActions.refreshTokenFailure())),
        ),
      ),
    );
  });
}
