import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Actions, createEffect, ofType, ROOT_EFFECTS_INIT } from '@ngrx/effects';
import { catchError, exhaustMap, map, of, switchMap, tap } from 'rxjs';

import { AuthActions } from './auth.actions';
import { AuthService } from '../../../core/services/auth.service';
import { ApiError } from '../../../core/models/auth/api-error.model';

const REFRESH_TOKEN_KEY = 'refreshToken';

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
      switchMap(() => {
        const token = localStorage.getItem(REFRESH_TOKEN_KEY);
        return token ? of(AuthActions.refreshToken()) : of(AuthActions.initSessionComplete());
      }),
    );
  });

  login$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(AuthActions.login),
      exhaustMap(({ request }) =>
        this.authService.login(request).pipe(
          map((response) => AuthActions.loginSuccess({ response })),
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

  loginOrRegisterSuccess$ = createEffect(
    () => {
      return this.actions$.pipe(
        ofType(AuthActions.loginSuccess, AuthActions.registerSuccess),
        tap(({ response }) => {
          localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
          this.router.navigate(['/dashboard']);
        }),
      );
    },
    { dispatch: false },
  );

  logout$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(AuthActions.logout),
      exhaustMap(() => {
        const token = localStorage.getItem(REFRESH_TOKEN_KEY) ?? '';
        return this.authService.logout(token).pipe(
          map(() => AuthActions.logoutSuccess()),
          catchError(() => of(AuthActions.logoutSuccess())),
        );
      }),
    );
  });

  logoutOrRefreshFailure$ = createEffect(
    () => {
      return this.actions$.pipe(
        ofType(AuthActions.logoutSuccess, AuthActions.refreshTokenFailure),
        tap(() => {
          localStorage.removeItem(REFRESH_TOKEN_KEY);
          this.router.navigate(['/login']);
        }),
      );
    },
    { dispatch: false },
  );

  refreshToken$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(AuthActions.refreshToken),
      exhaustMap(() => {
        const token = localStorage.getItem(REFRESH_TOKEN_KEY);
        if (!token) {
          return of(AuthActions.refreshTokenFailure());
        }
        return this.authService.refresh(token).pipe(
          tap(({ refreshToken }) => localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)),
          map((response) => AuthActions.refreshTokenSuccess({ response })),
          catchError(() => of(AuthActions.refreshTokenFailure())),
        );
      }),
    );
  });
}
