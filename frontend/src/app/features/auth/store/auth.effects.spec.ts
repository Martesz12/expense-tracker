import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideMockActions } from '@ngrx/effects/testing';
import { Observable, firstValueFrom, of } from 'rxjs';

import { AuthEffects } from './auth.effects';
import { AuthActions } from './auth.actions';
import { AuthService } from '../../../core/services/auth.service';
import { AuthResponse } from '../../../core/models/auth/auth-response.model';
import { User } from '../../../core/models/auth/user.model';

const mockUser: User = { id: '1', name: 'Test', email: 'test@example.com', homeCurrency: 'USD' };
const mockResponse: AuthResponse = {
  accessToken: 'access',
  refreshToken: 'refresh',
  user: mockUser,
};

describe('AuthEffects', () => {
  let actions$: Observable<unknown>;
  let effects: AuthEffects;
  let mockRouter: { navigate: ReturnType<typeof vi.fn>; url: string };
  let mockAuthService: {
    login: ReturnType<typeof vi.fn>;
    register: ReturnType<typeof vi.fn>;
    refresh: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    mockRouter = { navigate: vi.fn().mockResolvedValue(true), url: '/dashboard' };
    mockAuthService = { login: vi.fn(), register: vi.fn(), refresh: vi.fn(), logout: vi.fn() };
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        AuthEffects,
        provideMockActions(() => actions$),
        { provide: Router, useValue: mockRouter },
        { provide: AuthService, useValue: mockAuthService },
      ],
    });

    effects = TestBed.inject(AuthEffects);
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

    it('saves the refresh token to localStorage', async () => {
      actions$ = of(AuthActions.loginSuccess({ response: mockResponse }));

      await firstValueFrom(effects.loginSuccess$);

      expect(localStorage.getItem('refreshToken')).toBe('refresh');
    });
  });

  describe('registerSuccess$', () => {
    it('always navigates to /dashboard', async () => {
      actions$ = of(AuthActions.registerSuccess({ response: mockResponse }));

      await firstValueFrom(effects.registerSuccess$);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/dashboard']);
    });

    it('saves the refresh token to localStorage', async () => {
      actions$ = of(AuthActions.registerSuccess({ response: mockResponse }));

      await firstValueFrom(effects.registerSuccess$);

      expect(localStorage.getItem('refreshToken')).toBe('refresh');
    });
  });

  describe('logoutOrRefreshFailure$', () => {
    it('navigates to /login without returnUrl on intentional logout', async () => {
      actions$ = of(AuthActions.logoutSuccess());

      await firstValueFrom(effects.logoutOrRefreshFailure$);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/login'], undefined);
    });

    it('navigates to /login with returnUrl on refresh token failure', async () => {
      mockRouter.url = '/dashboard';
      actions$ = of(AuthActions.refreshTokenFailure());

      await firstValueFrom(effects.logoutOrRefreshFailure$);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/login'], {
        queryParams: { returnUrl: '/dashboard' },
      });
    });

    it('does not add returnUrl when already on /login on refresh failure', async () => {
      mockRouter.url = '/login';
      actions$ = of(AuthActions.refreshTokenFailure());

      await firstValueFrom(effects.logoutOrRefreshFailure$);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/login'], undefined);
    });

    it('does not add returnUrl when on /login with query params on refresh failure', async () => {
      mockRouter.url = '/login?returnUrl=%2Fdashboard';
      actions$ = of(AuthActions.refreshTokenFailure());

      await firstValueFrom(effects.logoutOrRefreshFailure$);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/login'], undefined);
    });

    it('removes the refresh token from localStorage', async () => {
      localStorage.setItem('refreshToken', 'old-token');
      actions$ = of(AuthActions.logoutSuccess());

      await firstValueFrom(effects.logoutOrRefreshFailure$);

      expect(localStorage.getItem('refreshToken')).toBeNull();
    });
  });
});
