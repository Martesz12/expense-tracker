import { Injectable, inject } from '@angular/core';
import { HttpHandlerFn, HttpRequest, HttpEvent } from '@angular/common/http';
import { Store } from '@ngrx/store';
import { Actions, ofType } from '@ngrx/effects';
import { BehaviorSubject, EMPTY, Observable } from 'rxjs';
import { filter, switchMap, take } from 'rxjs';

import { selectAccessToken } from '../../features/auth/store/auth.reducer';
import { AuthActions } from '../../features/auth/store/auth.actions';

function addToken(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

@Injectable({ providedIn: 'root' })
export class TokenRefreshService {
  private readonly store = inject(Store);
  private readonly actions$ = inject(Actions);

  private isRefreshing = false;
  private readonly refreshDone$ = new BehaviorSubject<boolean>(false);

  handleUnauthorized(
    req: HttpRequest<unknown>,
    next: HttpHandlerFn,
  ): Observable<HttpEvent<unknown>> {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshDone$.next(false);
      this.store.dispatch(AuthActions.refreshToken());

      return this.actions$.pipe(
        ofType(AuthActions.refreshTokenSuccess, AuthActions.refreshTokenFailure),
        take(1),
        switchMap((action) => {
          this.isRefreshing = false;
          this.refreshDone$.next(true);

          if (action.type === AuthActions.refreshTokenFailure.type) {
            // Effect handles logout + navigation
            return EMPTY;
          }

          return this.store.select(selectAccessToken).pipe(
            take(1),
            switchMap((token) => next(addToken(req, token!))),
          );
        }),
      );
    }

    // Another refresh is already in flight — wait for it to complete
    return this.refreshDone$.pipe(
      filter((done) => done),
      take(1),
      switchMap(() =>
        this.store.select(selectAccessToken).pipe(
          take(1),
          switchMap((token) => (token ? next(addToken(req, token)) : EMPTY)),
        ),
      ),
    );
  }
}
