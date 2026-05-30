import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

interface NavItem {
  path: string;
  icon: string;
  label: string;
}

@Component({
  selector: 'app-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  readonly navItems: NavItem[] = [
    { path: '/dashboard', icon: '⊞', label: 'Dashboard' },
    { path: '/accounts', icon: '🏦', label: 'Accounts' },
    { path: '/transactions', icon: '↕', label: 'Transactions' },
    { path: '/categories', icon: '🏷', label: 'Categories' },
    { path: '/budgets', icon: '◎', label: 'Budgets' },
    { path: '/reports', icon: '📊', label: 'Reports' },
    { path: '/recurring', icon: '🔁', label: 'Recurring' },
  ];
}
