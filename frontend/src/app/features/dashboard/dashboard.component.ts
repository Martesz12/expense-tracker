import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1 class="text-page-title">Dashboard</h1>
    <p class="text-secondary">Welcome to Expense Tracker.</p>
  `,
})
export class DashboardComponent {}
