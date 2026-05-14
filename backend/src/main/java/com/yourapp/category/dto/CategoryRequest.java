package com.yourapp.category.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CategoryRequest(
        @NotBlank String name,
        @NotBlank String type,
        String icon,
        String color,
        UUID parentId
) {}
