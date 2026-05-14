package com.yourapp.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TransactionRequest(
        @NotBlank String type,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        BigDecimal exchangeRate,
        @NotNull UUID fromAccountId,
        UUID toAccountId,
        UUID categoryId,
        String note,
        @NotNull OffsetDateTime transactionDate,
        List<UUID> tagIds
) {}
