package com.yourapp.auth;

import com.yourapp.auth.dto.AuthResponse;

record TokenPair(AuthResponse response, String rawRefreshToken) {}
