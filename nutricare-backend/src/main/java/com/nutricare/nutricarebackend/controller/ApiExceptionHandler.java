package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException exception, HttpServletRequest request) {
        String reason = exception.getReason() == null
                ? exception.getStatusCode().toString()
                : exception.getReason();

        return ResponseEntity.status(exception.getStatusCode())
                .body(ApiErrorResponse.builder()
                        .success(false)
                        .reason(reason)
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String reason = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (reason.isBlank()) {
            reason = "Invalid request payload";
        }

        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.builder()
                        .success(false)
                        .reason(reason)
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException exception, HttpServletRequest request) {
        String reason = exception.getMostSpecificCause() != null
                ? exception.getMostSpecificCause().getMessage()
                : "Invalid request payload";
        if (reason == null || reason.isBlank()) {
            reason = "Invalid request payload";
        }

        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.builder()
                        .success(false)
                        .reason(reason)
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        String reason = exception.getMessage() == null ? "Unexpected error" : exception.getMessage();
        
        // Return a clean message if database constraint violation/data truncation happens
        if (exception.getClass().getName().contains("JpaSystemException") || 
            exception.getClass().getName().contains("DataIntegrityViolationException") ||
            reason.contains("Data truncated") ||
            reason.contains("booking_status") ||
            reason.contains("DataTruncation")) {
            reason = "Invalid status or booking status value. Database constraint violated.";
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.builder()
                            .success(false)
                            .reason(reason)
                            .path(request.getRequestURI())
                            .build());
        }

        return ResponseEntity.status(500)
                .body(ApiErrorResponse.builder()
                        .success(false)
                        .reason(reason)
                        .path(request.getRequestURI())
                        .build());
    }
}
