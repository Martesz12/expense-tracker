package com.yourapp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtConfig {
    private String secret;
    private int accessExpiryMinutes = 15;
    private int refreshExpiryDays = 30;
    private boolean refreshCookieSecure = false;
}
