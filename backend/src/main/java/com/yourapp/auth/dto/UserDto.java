package com.yourapp.auth.dto;

import java.util.UUID;

public record UserDto(UUID id, String name, String email, String homeCurrency) {}
