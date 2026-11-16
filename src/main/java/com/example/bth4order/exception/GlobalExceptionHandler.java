package com.example.bth4order.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleStatus(ResponseStatusException ex, HttpServletRequest req) {
        return error(ex.getStatusCode().value(), ex.getReason(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst().orElse("Invalid request");
        return error(400, message, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return error(400, "Invalid JSON body or status value", req);
    }

    private ResponseEntity<ApiError> error(int status, String message, HttpServletRequest req) {
        HttpStatus code = HttpStatus.valueOf(status);
        return ResponseEntity.status(code).body(new ApiError(LocalDateTime.now(),
                status, code.getReasonPhrase(), message, req.getRequestURI()));
    }
}
