package com.yourapp.auth;

import com.yourapp.category.CategoryRepository;
import com.yourapp.config.JwtConfig;
import com.yourapp.user.User;
import com.yourapp.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock JwtUtil jwtUtil;
    @Mock JwtConfig jwtConfig;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks AuthService authService;

    private User buildUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Alice")
                .email("alice@example.com")
                .password("encoded-pw")
                .build();
    }

    @Test
    void register_success_savesUserAndSeedsCategories() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-pw");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u = User.builder().id(UUID.randomUUID()).name(u.getName())
                    .email(u.getEmail()).password(u.getPassword()).build();
            return u;
        });
        when(jwtConfig.getRefreshExpiryDays()).thenReturn(30);
        when(jwtUtil.generateAccessToken(any())).thenReturn("access-token");

        TokenPair pair = authService.register("Alice", "alice@example.com", "password123");

        assertThat(pair.response().accessToken()).isEqualTo("access-token");
        verify(categoryRepository, times(8)).save(any());
    }

    @Test
    void register_duplicateEmail_throwsConflict409() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("Alice", "alice@example.com", "password123"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void login_success_returnsAuthResponse() {
        User user = buildUser();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-pw")).thenReturn(true);
        when(jwtConfig.getRefreshExpiryDays()).thenReturn(30);
        when(jwtUtil.generateAccessToken(user.getId())).thenReturn("access-token");

        TokenPair pair = authService.login("alice@example.com", "password123");

        assertThat(pair.response().accessToken()).isEqualTo("access-token");
    }

    @Test
    void login_wrongEmail_throwsUnauthorized401() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("nobody@example.com", "pw"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void login_wrongPassword_throwsUnauthorized401() {
        User user = buildUser();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpw", "encoded-pw")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("alice@example.com", "wrongpw"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void refresh_success_revokesOldAndIssuesNew() {
        User user = buildUser();
        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash("dummy-hash")
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
        when(jwtConfig.getRefreshExpiryDays()).thenReturn(30);
        when(jwtUtil.generateAccessToken(user.getId())).thenReturn("new-access");

        TokenPair pair = authService.refresh("some-raw-token");

        assertThat(pair.response().accessToken()).isEqualTo("new-access");
        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository, atLeast(2)).save(any());
    }

    @Test
    void refresh_unknownToken_throwsUnauthorized401() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("unknown-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void refresh_revokedToken_throwsUnauthorized401() {
        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(buildUser())
                .tokenHash("hash")
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .revoked(true)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh("some-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void refresh_expiredToken_throwsUnauthorized401() {
        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(buildUser())
                .tokenHash("hash")
                .expiresAt(OffsetDateTime.now().minusDays(1))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh("some-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void logout_validToken_marksRevoked() {
        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(buildUser())
                .tokenHash("hash")
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        authService.logout("some-raw-token");

        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void logout_unknownToken_doesNothing() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        authService.logout("unknown-token");

        verify(refreshTokenRepository, never()).save(any());
    }
}
