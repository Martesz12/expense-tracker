package com.yourapp.category.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String type,
        String icon,
        String color,
        UUID parentId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
