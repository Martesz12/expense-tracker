import { AccountTypePipe } from './account-type.pipe';

describe('AccountTypePipe', () => {
  const pipe = new AccountTypePipe();

  it('transforms CASH to Cash', () => expect(pipe.transform('CASH')).toBe('Cash'));
  it('transforms CREDIT_CARD to Credit Card', () =>
    expect(pipe.transform('CREDIT_CARD')).toBe('Credit Card'));
  it('transforms unknown value to the original string', () =>
    expect(pipe.transform('FOO')).toBe('FOO'));
});
