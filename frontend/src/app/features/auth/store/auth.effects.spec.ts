import { TestBed } from '@angular/core/testing';
import { Location } from '@angular/common';
import { Router } from '@angular/router';
import { provideMockActions } from '@ngrx/effects/testing';
import { Observable, firstValueFrom, of, throwError } from 'rxjs';

import { AuthEffects } from './auth.effects';
import { AuthActions } from './auth.actions';
import { AuthService } from '../../../core/services/auth.service';
import { AuthResponse } from '../../../core/models/auth/auth-response.model';
import { User } from '../../../core/models/auth/user.model';
import { ROOT_EFFECTS_INIT } from '@ngrx/effects';

const mockUser: User = { id: '1', name: 'Test', email: 'test@example.com', homeCurrency: 'USD' };
const mockResponse: AuthResponse = {
  accessToken: 'access',
  user: mockUser,
};

describe('AuthEffects', () => {
  let actions$: Observable<unknown>;
  let effects: AuthEffects;
  let mockRouter: { navigate: ReturnType<typeof vi.fn> };
  let mockLocation: { path: ReturnType<typeof vi.fn> };
  let mockAuthService: {
    login: ReturnType<typeof vi.fn>;
    register: ReturnType<typeof vi.fn>;
    refresh: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    mockRouter = { navigate: vi.fn().mockResolvedValue(true) };
    mockLocation = { path: vi.fn().mockReturnValue('/dashboard') };
    mockAuthService = { login: vi.fn(), register: vi.fn(), refresh: vi.fn(), logout: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        AuthEffects,
        provideMockActions(() => actions$),
        { provide: Router, useValue: mockRouter },
        { provide: Location, useValue: mockLocation },
        { provide: AuthService, useValue: mockAuthService },
      ],
    });

    effects = TestBed.inject(AuthEffects);
  });

  describe('initSession$', () => {
    it('always dispatches refreshToken regardless of cookie state', async () => {
      actions$ = of({ type: ROOT_EFFECTS_INIT });

      const result = await firstValueFrom(effects.initSession$);

      expect(result).toEqual(AuthActions.refreshToken());
    });
  });

  describe('login$', () => {
    it('forwards returnUrl to loginSuccess', async () => {
      mockAuthService.login.mockReturnValue(of(mockResponse));

      actions$ = of(
        AuthActions.login({
          request: { email: 'test@example.com', password: 'password123' },
          returnUrl: '/transactions',
        }),
      );

      const result = await firstValueFrom(effects.login$);

      expect(result).toEqual(
        AuthActions.loginSuccess({ response: mockResponse, returnUrl: '/transactions' }),
      );
    });
  });

  describe('loginSuccess$', () => {
    it('navigates to returnUrl when a valid relative URL is provided', async () => {
      actions$ = of(
        AuthActions.loginSuccess({ response: mockResponse, returnUrl: '/transactions' }),
      );

      await firstValueFrom(effects.loginSuccess$);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/transactions']);
    });

    it('navigates to /dashboard when no returnUrl is provided', async () => {
      actions$ = of(AuthActions.loginSuccess({ response: mockResponse }));

      await firstValueFrom(effects.loginSuccess$);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/dashboard']);
    });

    it('navigates to /dashboard when returnUrl is an external URL', async () => {
      actions$ = of(
        AuthActions.loginSuccess({ response: mockResponse, returnUrl: 'http://evil.com' }),
      );

      await firstValueFrom(effects.loginSuccess$);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/dashboard']);
    });
  });

  describe('registerSuccess$', () => {
    it('always navigates to /dashboard', async () => {
      actions$ = of(AuthActions.registerSuccess({ response: mockResponse }));

      await firstValueFrom(effects.registerSuccess$);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/dashboard']);
    });
  });

  describe('logout$', () => {
    it('calls authService.logout() with no arguments and dispatches logoutSuccess', async () => {
      mockAuthService.logout.mockReturnValue(of(void 0));
      actions$ = of(AuthActions.logout());

      const result = await firstValueFrom(effects.logout$);

      expect(mockAuthService.logout).toHaveBeenCalledWith();
      expect(result).toEqual(AuthActions.logoutSuccess());
    });

    it('dispatches logoutSuccess even when logout API call fails', async () => {
      mockAuthService.logout.mockReturnValue(throwError(() => new Error('network error')));
      actions$ = of(AuthActions.logout());

      const result = await firstValueFrom(effects.logout$);

      expect(result).toEqual(AuthActions.logoutSuccess());
    });
  });

  describe('logoutOrRefreshFailure$', () => {
    it('navigates to /login without returnUrl on intentional logout', async () => {
      actions$ = of(AuthActions.logoutSuccess());

      await firstValueFrom(effects.logoutOrRefreshFailure$);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/login'], undefined);
    });

    it('navigates to /login with returnUrl on refresh token failure', async () => {
      mockLocation.path.mockReturnValue('/dashboard');
      actions$ = of(AuthActions.refreshTokenFailure());

      await firstValueFrom(effects.logoutOrRefreshFailure$);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/login'], {
        queryParams: { returnUrl: '/dashboard' },
      });
    });

    it('does not add returnUrl when already on /login on refresh failure', async () => {
      mockLocation.path.mockReturnValue('/login');
      actions$ = of(AuthActions.refreshTokenFailure());

      await firstValueFrom(effects.logoutOrRefreshFailure$);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/login'], undefined);
    });

    it('does not add returnUrl when on /login with query params on refresh failure', async () => {
      mockLocation.path.mockReturnValue('/login?returnUrl=%2Fdashboard');
      actions$ = of(AuthActions.refreshTokenFailure());

      await firstValueFrom(effects.logoutOrRefreshFailure$);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/login'], undefined);
    });
  });

  describe('refreshToken$', () => {
    it('calls authService.refresh() with no arguments and dispatches refreshTokenSuccess', async () => {
      mockAuthService.refresh.mockReturnValue(of(mockResponse));
      actions$ = of(AuthActions.refreshToken());

      const result = await firstValueFrom(effects.refreshToken$);

      expect(mockAuthService.refresh).toHaveBeenCalledWith();
      expect(result).toEqual(AuthActions.refreshTokenSuccess({ response: mockResponse }));
    });

    it('dispatches refreshTokenFailure when refresh API call fails', async () => {
      mockAuthService.refresh.mockReturnValue(throwError(() => new Error('401')));
      actions$ = of(AuthActions.refreshToken());

      const result = await firstValueFrom(effects.refreshToken$);

      expect(result).toEqual(AuthActions.refreshTokenFailure());
    });
  });
});
