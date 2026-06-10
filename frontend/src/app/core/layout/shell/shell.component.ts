import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, ViewChild, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Store } from '@ngrx/store';
import { Popover } from 'primeng/popover';

import { User } from '../../models/auth/user.model';
import { AuthActions } from '../../../features/auth/store/auth.actions';
import { selectUser } from '../../../features/auth/store/auth.reducer';

interface NavItem {
  path: string;
  icon: string;
  label: string;
}

@Component({
  selector: 'app-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AsyncPipe, Popover],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  @ViewChild('op') private readonly userPopover!: Popover;

  private readonly store = inject(Store);

  readonly user$ = this.store.select(selectUser);

  readonly navItems: NavItem[] = [
    { path: '/dashboard', icon: '⊞', label: 'Dashboard' },
    { path: '/accounts', icon: '🏦', label: 'Accounts' },
    { path: '/transactions', icon: '↕', label: 'Transactions' },
    { path: '/categories', icon: '🏷', label: 'Categories' },
    { path: '/budgets', icon: '◎', label: 'Budgets' },
    { path: '/reports', icon: '📊', label: 'Reports' },
    { path: '/recurring', icon: '🔁', label: 'Recurring' },
  ];

  avatarInitial(user: User | null): string {
    return user?.name?.[0]?.toUpperCase() ?? '?';
  }

  logout(): void {
    this.userPopover.hide();
    this.store.dispatch(AuthActions.logout());
  }
}
