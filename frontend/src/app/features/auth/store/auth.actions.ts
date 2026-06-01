import { createActionGroup, emptyProps, props } from '@ngrx/store';

import { AuthResponse } from '../../../core/models/auth/auth-response.model';
import { LoginRequest } from '../../../core/models/auth/login-request.model';
import { RegisterRequest } from '../../../core/models/auth/register-request.model';

export const AuthActions = createActionGroup({
  source: 'Auth',
  events: {
    Login: props<{ request: LoginRequest }>(),
    'Login Success': props<{ response: AuthResponse }>(),
    'Login Failure': props<{ error: string }>(),

    Register: props<{ request: RegisterRequest }>(),
    'Register Success': props<{ response: AuthResponse }>(),
    'Register Failure': props<{ error: string }>(),

    Logout: emptyProps(),
    'Logout Success': emptyProps(),

    'Refresh Token': emptyProps(),
    'Refresh Token Success': props<{ response: AuthResponse }>(),
    'Refresh Token Failure': emptyProps(),

    'Init Session': emptyProps(),
    'Init Session Complete': emptyProps(),
  },
});
