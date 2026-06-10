import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { MemoizedSelector } from '@ngrx/store';
import { MockStore, provideMockStore } from '@ngrx/store/testing';

import { User } from '../../models/auth/user.model';
import { AuthActions } from '../../../features/auth/store/auth.actions';
import { selectUser } from '../../../features/auth/store/auth.reducer';
import { ShellComponent } from './shell.component';

const mockUser: User = {
  id: '1',
  name: 'Martin B',
  email: 'martin@example.com',
  homeCurrency: 'EUR',
};

async function setup(user: User | null = mockUser) {
  await TestBed.configureTestingModule({
    imports: [ShellComponent],
    providers: [provideRouter([]), provideNoopAnimations(), provideMockStore({ initialState: {} })],
  }).compileComponents();

  const store = TestBed.inject(MockStore);
  store.overrideSelector(selectUser as MemoizedSelector<object, User | null>, user);
  store.refreshState();

  const fixture = TestBed.createComponent(ShellComponent);
  fixture.detectChanges();

  return { fixture, store };
}

describe('ShellComponent', () => {
  afterEach(() => {
    TestBed.inject(MockStore).resetSelectors();
  });

  it('renders the sidebar', async () => {
    const { fixture } = await setup();
    expect(fixture.debugElement.query(By.css('.sidebar'))).toBeTruthy();
  });

  it('renders a router-outlet', async () => {
    const { fixture } = await setup();
    expect(fixture.debugElement.query(By.css('router-outlet'))).toBeTruthy();
  });

  it('renders nav items for all 7 main routes', async () => {
    const { fixture } = await setup();
    const items = fixture.debugElement.queryAll(By.css('.sidebar-nav-item'));
    expect(items.length).toBe(7);
  });

  it('renders the logo mark', async () => {
    const { fixture } = await setup();
    expect(fixture.debugElement.query(By.css('.sidebar-logo'))).toBeTruthy();
  });

  it('renders the user avatar with the first letter of the user name', async () => {
    const { fixture } = await setup();
    const avatar = fixture.debugElement.query(By.css('.user-avatar'));
    expect(avatar).toBeTruthy();
    expect(avatar.nativeElement.textContent.trim()).toBe('M');
  });

  it('renders ? in the avatar when no user is loaded', async () => {
    const { fixture } = await setup(null);
    const avatar = fixture.debugElement.query(By.css('.user-avatar'));
    expect(avatar.nativeElement.textContent.trim()).toBe('?');
  });

  it('dispatches logout action when logout() is called', async () => {
    const { fixture, store } = await setup();
    const dispatchSpy = vi.spyOn(store, 'dispatch');

    fixture.componentInstance.logout();

    expect(dispatchSpy).toHaveBeenCalledWith(AuthActions.logout());
  });
});
