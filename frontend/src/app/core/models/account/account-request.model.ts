import { AccountType } from './account.model';

export interface AccountRequest {
  name: string;
  type: AccountType;
  currency: string;
  initialBalance?: number | null;
  color?: string | null;
  icon?: string | null;
}
