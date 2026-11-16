package com.example.bth4order.exception;

import java.time.LocalDateTime;

public record ApiError(LocalDateTime timestamp, int status, String error, String message, String path) { }
