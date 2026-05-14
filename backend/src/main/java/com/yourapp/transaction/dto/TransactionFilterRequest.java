package com.yourapp.transaction.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TransactionFilterRequest(
        LocalDate from,
        LocalDate to,
        UUID accountId,
        UUID categoryId,
        String type,
        List<UUID> tagIds,
        String search
) {}
