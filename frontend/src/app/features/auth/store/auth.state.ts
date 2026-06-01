import { User } from '../../../core/models/auth/user.model';

export interface AuthState {
  user: User | null;
  accessToken: string | null;
  loginError: string | null;
  registerError: string | null;
  loading: boolean;
  initialized: boolean;
}

export const initialAuthState: AuthState = {
  user: null,
  accessToken: null,
  loginError: null,
  registerError: null,
  loading: false,
  initialized: false,
};
