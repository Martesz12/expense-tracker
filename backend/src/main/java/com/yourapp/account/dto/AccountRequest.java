package com.yourapp.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AccountRequest(
        @NotBlank String name,
        @NotBlank String type,
        @NotBlank @Size(min = 3, max = 3) String currency,
        BigDecimal initialBalance,
        String color,
        String icon
) {}
