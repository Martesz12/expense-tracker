package com.yourapp.auth;

import com.yourapp.auth.dto.AuthResponse;
import com.yourapp.auth.dto.LoginRequest;
import com.yourapp.auth.dto.RegisterRequest;
import com.yourapp.config.JwtConfig;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtConfig jwtConfig;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse httpResponse) {
        TokenPair pair = authService.register(request.name(), request.email(), request.password());
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(pair.rawRefreshToken()).toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(pair.response());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse httpResponse) {
        TokenPair pair = authService.login(request.email(), request.password());
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(pair.rawRefreshToken()).toString());
        return ResponseEntity.ok(pair.response());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse httpResponse) {
        if (refreshToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing refresh token");
        }
        TokenPair pair = authService.refresh(refreshToken);
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(pair.rawRefreshToken()).toString());
        return ResponseEntity.ok(pair.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse httpResponse) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie("", 0).toString());
        return ResponseEntity.noContent().build();
    }

    private ResponseCookie refreshCookie(String value) {
        return refreshCookie(value, (long) jwtConfig.getRefreshExpiryDays() * 86400);
    }

    private ResponseCookie refreshCookie(String value, long maxAgeSecs) {
        return ResponseCookie.from("refreshToken", value)
                .httpOnly(true)
                .secure(jwtConfig.isRefreshCookieSecure())
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAgeSecs)
                .build();
    }
}
