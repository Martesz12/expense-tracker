import {
  HttpErrorResponse,
  HttpHandlerFn,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { throwError } from 'rxjs';
import { catchError } from 'rxjs';

import { selectAccessToken } from '../../features/auth/store/auth.reducer';
import { TokenRefreshService } from './token-refresh.service';

function addToken(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

export const jwtInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
) => {
  const store = inject(Store);
  const refreshService = inject(TokenRefreshService);

  if (req.url.includes('/auth/')) {
    return next(req);
  }

  const token = store.selectSignal(selectAccessToken)();
  return next(token ? addToken(req, token) : req).pipe(
    catchError((err: unknown) => {
      if (!(err instanceof HttpErrorResponse) || err.status !== 401) {
        return throwError(() => err);
      }
      return refreshService.handleUnauthorized(req, next);
    }),
  );
};
