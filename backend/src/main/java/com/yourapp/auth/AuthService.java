package com.yourapp.auth;

import com.yourapp.auth.dto.AuthResponse;
import com.yourapp.auth.dto.UserDto;
import com.yourapp.category.Category;
import com.yourapp.category.CategoryRepository;
import com.yourapp.config.JwtConfig;
import com.yourapp.user.User;
import com.yourapp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CategoryRepository categoryRepository;
    private final JwtUtil jwtUtil;
    private final JwtConfig jwtConfig;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TokenPair register(String name, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .build();
        userRepository.save(user);

        seedDefaultCategories(user);

        return issueTokens(user);
    }

    @Transactional
    public TokenPair login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return issueTokens(user);
    }

    @Transactional
    public TokenPair refresh(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (stored.isRevoked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token revoked");
        }
        if (stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private TokenPair issueTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId());
        String rawRefreshToken = UUID.randomUUID().toString();
        String tokenHash = sha256(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(OffsetDateTime.now().plusDays(jwtConfig.getRefreshExpiryDays()))
                .build();
        refreshTokenRepository.save(refreshToken);

        return new TokenPair(new AuthResponse(accessToken, toDto(user)), rawRefreshToken);
    }

    private void seedDefaultCategories(User user) {
        List<Object[]> defaults = List.of(
                new Object[]{"Food", "EXPENSE", "🍔", "#FF6B6B"},
                new Object[]{"Transport", "EXPENSE", "🚌", "#4ECDC4"},
                new Object[]{"Housing", "EXPENSE", "🏠", "#45B7D1"},
                new Object[]{"Entertainment", "EXPENSE", "🎬", "#96CEB4"},
                new Object[]{"Health", "EXPENSE", "💊", "#FFEAA7"},
                new Object[]{"Shopping", "EXPENSE", "🛍️", "#DDA0DD"},
                new Object[]{"Salary", "INCOME", "💼", "#98D8C8"},
                new Object[]{"Freelance", "INCOME", "💻", "#F7DC6F"}
        );

        for (Object[] def : defaults) {
            categoryRepository.save(Category.builder()
                    .user(user)
                    .name((String) def[0])
                    .type((String) def[1])
                    .icon((String) def[2])
                    .color((String) def[3])
                    .build());
        }
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getHomeCurrency());
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
