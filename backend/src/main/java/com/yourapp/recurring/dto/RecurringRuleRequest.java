package com.yourapp.recurring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecurringRuleRequest(
        @NotBlank String type,
        @NotNull BigDecimal amount,
        @NotBlank String currency,
        BigDecimal exchangeRate,
        @NotNull UUID fromAccountId,
        UUID toAccountId,
        UUID categoryId,
        @NotBlank String frequency,
        @NotNull Integer intervalValue,
        @NotNull LocalDate startDate,
        LocalDate endDate
) {}
