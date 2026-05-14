package com.yourapp.account.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String name,
        String type,
        String currency,
        BigDecimal initialBalance,
        BigDecimal balance,
        String color,
        String icon,
        boolean archived,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
