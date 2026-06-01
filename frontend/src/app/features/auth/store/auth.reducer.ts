import { createFeature, createReducer, on } from '@ngrx/store';

import { AuthActions } from './auth.actions';
import { AuthState, initialAuthState } from './auth.state';

export const authFeature = createFeature({
  name: 'auth',
  reducer: createReducer(
    initialAuthState,

    on(AuthActions.login, AuthActions.register, (state): AuthState => ({
      ...state,
      loading: true,
      loginError: null,
      registerError: null,
    })),

    on(
      AuthActions.loginSuccess,
      AuthActions.registerSuccess,
      AuthActions.refreshTokenSuccess,
      (state, { response }): AuthState => ({
        ...state,
        loading: false,
        user: response.user,
        accessToken: response.accessToken,
        loginError: null,
        registerError: null,
        initialized: true,
      }),
    ),

    on(AuthActions.loginFailure, (state, { error }): AuthState => ({
      ...state,
      loading: false,
      loginError: error,
    })),

    on(AuthActions.registerFailure, (state, { error }): AuthState => ({
      ...state,
      loading: false,
      registerError: error,
    })),

    on(AuthActions.logoutSuccess, AuthActions.refreshTokenFailure, (state): AuthState => ({
      ...state,
      user: null,
      accessToken: null,
      loading: false,
      initialized: true,
    })),

    on(AuthActions.initSessionComplete, (state): AuthState => ({
      ...state,
      initialized: true,
    })),
  ),
});

export const {
  name: authFeatureKey,
  reducer: authReducer,
  selectAuthState,
  selectUser,
  selectAccessToken,
  selectLoading,
  selectLoginError,
  selectRegisterError,
  selectInitialized,
} = authFeature;
