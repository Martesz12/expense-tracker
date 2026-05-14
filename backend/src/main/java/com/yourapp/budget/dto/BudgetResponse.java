package com.yourapp.budget.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BudgetResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        String periodType,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal amountLimit,
        String currency,
        BigDecimal amountSpent,
        BigDecimal percentUsed,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
