import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'accountType', standalone: true })
export class AccountTypePipe implements PipeTransform {
  private readonly labels: Record<string, string> = {
    CASH: 'Cash',
    BANK: 'Bank',
    CREDIT_CARD: 'Credit Card',
    SAVINGS: 'Savings',
    INVESTMENT: 'Investment',
    OTHER: 'Other',
  };

  transform(type: string): string {
    return this.labels[type] ?? type;
  }
}
