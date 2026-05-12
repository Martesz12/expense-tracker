package com.yourapp.common.dto;

public record ErrorResponse(int status, String error, String message, String timestamp) {}
