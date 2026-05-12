package com.yourapp.auth.dto;

public record AuthResponse(String accessToken, String refreshToken, UserDto user) {}
