import { createSelector } from '@ngrx/store';

import { selectUser, selectAccessToken } from './auth.reducer';

export const selectIsAuthenticated = createSelector(
  selectUser,
  selectAccessToken,
  (user, token) => user !== null && token !== null,
);
