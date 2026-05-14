package com.yourapp.auth;

import com.yourapp.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-chars!!";

    private JwtUtil jwtUtil;
    private JwtConfig config;

    @BeforeEach
    void setUp() {
        config = new JwtConfig();
        config.setSecret(SECRET);
        config.setAccessExpiryMinutes(15);
        config.setRefreshExpiryDays(30);
        jwtUtil = new JwtUtil(config);
    }

    @Test
    void generateAccessToken_returnsNonNullToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void extractUserId_returnsCorrectUuid() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId);
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void isValid_returnsTrueForFreshToken() {
        String token = jwtUtil.generateAccessToken(UUID.randomUUID());
        assertThat(jwtUtil.isValid(token)).isTrue();
    }

    @Test
    void isValid_returnsFalseForTamperedToken() {
        String token = jwtUtil.generateAccessToken(UUID.randomUUID());
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtUtil.isValid(tampered)).isFalse();
    }

    @Test
    void isValid_returnsFalseForExpiredToken() throws InterruptedException {
        JwtConfig expiredConfig = new JwtConfig();
        expiredConfig.setSecret(SECRET);
        expiredConfig.setAccessExpiryMinutes(0);
        expiredConfig.setRefreshExpiryDays(30);
        JwtUtil expiredJwtUtil = new JwtUtil(expiredConfig);

        String token = expiredJwtUtil.generateAccessToken(UUID.randomUUID());
        Thread.sleep(10);
        assertThat(expiredJwtUtil.isValid(token)).isFalse();
    }

    @Test
    void isValid_returnsFalseForArbitraryString() {
        assertThat(jwtUtil.isValid("not.a.jwt")).isFalse();
    }
}
