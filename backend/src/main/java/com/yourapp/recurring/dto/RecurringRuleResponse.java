package com.yourapp.recurring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RecurringRuleResponse(
        UUID id,
        String type,
        BigDecimal amount,
        String currency,
        BigDecimal exchangeRate,
        UUID fromAccountId,
        String fromAccountName,
        UUID toAccountId,
        String toAccountName,
        UUID categoryId,
        String categoryName,
        String frequency,
        int intervalValue,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate nextOccurrence,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
