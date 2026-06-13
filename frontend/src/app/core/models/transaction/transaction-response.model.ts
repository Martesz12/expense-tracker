export interface TransactionResponse {
  id: string;
  type: 'INCOME' | 'EXPENSE' | 'TRANSFER';
  amount: number;
  currency: string;
  fromAccountId: string;
  toAccountId: string | null;
  categoryId: string | null;
  categoryName: string | null;
  note: string | null;
  transactionDate: string;
}
