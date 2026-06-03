import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { MockStore, provideMockStore } from '@ngrx/store/testing';
import { Observable, firstValueFrom } from 'rxjs';

import { authGuard } from './auth.guard';
import { selectInitialized } from '../../features/auth/store/auth.reducer';
import { selectIsAuthenticated } from '../../features/auth/store/auth.selectors';

describe('authGuard', () => {
  let store: MockStore;
  let router: Router;
  const mockRoute = {} as ActivatedRouteSnapshot;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideMockStore(), provideRouter([])],
    });
    store = TestBed.inject(MockStore);
    router = TestBed.inject(Router);
  });

  afterEach(() => store.resetSelectors());

  function runGuard(url = '/dashboard') {
    return TestBed.runInInjectionContext(() =>
      authGuard(mockRoute, { url } as RouterStateSnapshot),
    ) as Observable<boolean | UrlTree>;
  }

  it('allows navigation when authenticated and initialized', async () => {
    store.overrideSelector(selectInitialized, true);
    store.overrideSelector(selectIsAuthenticated, true);
    store.refreshState();

    expect(await firstValueFrom(runGuard())).toBe(true);
  });

  it('redirects to /login with returnUrl when not authenticated', async () => {
    store.overrideSelector(selectInitialized, true);
    store.overrideSelector(selectIsAuthenticated, false);
    store.refreshState();

    const result = await firstValueFrom(runGuard('/dashboard'));

    expect(result).toBeInstanceOf(UrlTree);
    expect(router.serializeUrl(result as UrlTree)).toBe('/login?returnUrl=%2Fdashboard');
  });

  it('encodes the attempted path as returnUrl', async () => {
    store.overrideSelector(selectInitialized, true);
    store.overrideSelector(selectIsAuthenticated, false);
    store.refreshState();

    const result = await firstValueFrom(runGuard('/transactions'));

    expect(router.serializeUrl(result as UrlTree)).toBe('/login?returnUrl=%2Ftransactions');
  });

  it('does not emit while auth state is not initialized', () => {
    store.overrideSelector(selectInitialized, false);
    store.overrideSelector(selectIsAuthenticated, false);
    store.refreshState();

    let emitted = false;
    runGuard().subscribe(() => {
      emitted = true;
    });

    expect(emitted).toBe(false);
  });
});
