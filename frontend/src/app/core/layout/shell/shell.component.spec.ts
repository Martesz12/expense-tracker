import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { ShellComponent } from './shell.component';

describe('ShellComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('renders the sidebar', () => {
    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('.sidebar'))).toBeTruthy();
  });

  it('renders a router-outlet', () => {
    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('router-outlet'))).toBeTruthy();
  });

  it('renders nav items for all 7 main routes', () => {
    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    const items = fixture.debugElement.queryAll(By.css('.sidebar-nav-item'));
    expect(items.length).toBe(7);
  });

  it('renders the logo mark', () => {
    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('.sidebar-logo'))).toBeTruthy();
  });

  it('renders the user avatar', () => {
    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('.user-avatar'))).toBeTruthy();
  });
});
