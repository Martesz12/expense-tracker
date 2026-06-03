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

import { guestGuard } from './guest.guard';
import { selectInitialized } from '../../features/auth/store/auth.reducer';
import { selectIsAuthenticated } from '../../features/auth/store/auth.selectors';

describe('guestGuard', () => {
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

  function runGuard() {
    return TestBed.runInInjectionContext(() =>
      guestGuard(mockRoute, {} as RouterStateSnapshot),
    ) as Observable<boolean | UrlTree>;
  }

  it('allows navigation when not authenticated', async () => {
    store.overrideSelector(selectInitialized, true);
    store.overrideSelector(selectIsAuthenticated, false);
    store.refreshState();

    expect(await firstValueFrom(runGuard())).toBe(true);
  });

  it('redirects to /dashboard when already authenticated', async () => {
    store.overrideSelector(selectInitialized, true);
    store.overrideSelector(selectIsAuthenticated, true);
    store.refreshState();

    const result = await firstValueFrom(runGuard());

    expect(result).toBeInstanceOf(UrlTree);
    expect(router.serializeUrl(result as UrlTree)).toBe('/dashboard');
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
