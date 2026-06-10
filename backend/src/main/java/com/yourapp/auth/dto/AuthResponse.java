package com.yourapp.auth.dto;

public record AuthResponse(String accessToken, UserDto user) {}
