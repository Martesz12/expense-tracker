package com.yourapp.budget.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BudgetRequest(
        @NotNull UUID categoryId,
        @NotBlank String periodType,
        LocalDate periodStart,
        LocalDate periodEnd,
        @NotNull BigDecimal amountLimit,
        @NotBlank String currency
) {}
