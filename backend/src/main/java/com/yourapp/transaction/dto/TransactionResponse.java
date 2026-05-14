package com.yourapp.transaction.dto;

import com.yourapp.tag.dto.TagResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TransactionResponse(
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
        String note,
        OffsetDateTime transactionDate,
        UUID recurringRuleId,
        List<TagResponse> tags,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
