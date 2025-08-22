package com.cloudshareoriginal.controller;

import com.cloudshareoriginal.dto.EntityResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<EntityResponse<Void>> handleValidation(MethodArgumentNotValidException ex,
                                                                 HttpServletRequest request) {
        Map<String, Object> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        EntityResponse<Void> body = EntityResponse.<Void>builder()
                .success(false)
                .message("Validation failed")
                .errors(errors)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<EntityResponse<Void>> handleResponseStatus(ResponseStatusException ex,
                                                                     HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        EntityResponse<Void> body = EntityResponse.<Void>builder()
                .success(false)
                .message(ex.getReason() != null ? ex.getReason() : "Request failed")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(status != null ? status : HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<EntityResponse<Void>> handleBadCredentials(BadCredentialsException ex,
                                                                     HttpServletRequest request) {
        EntityResponse<Void> body = EntityResponse.<Void>builder()
                .success(false)
                .message("Invalid credentials")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<EntityResponse<Void>> handleAccessDenied(AccessDeniedException ex,
                                                                   HttpServletRequest request) {
        EntityResponse<Void> body = EntityResponse.<Void>builder()
                .success(false)
                .message("Access denied")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<EntityResponse<Void>> handleIllegalState(IllegalStateException ex,
                                                                  HttpServletRequest request) {
        EntityResponse<Void> body = EntityResponse.<Void>builder()
                .success(false)
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<EntityResponse<Void>> handleMissingParam(org.springframework.web.bind.MissingServletRequestParameterException ex,
                                                                   HttpServletRequest request) {
        Map<String, Object> errors = new LinkedHashMap<>();
        errors.put(ex.getParameterName(), "Required request parameter is missing");
        EntityResponse<Void> body = EntityResponse.<Void>builder()
                .success(false)
                .message("Missing required request parameter")
                .errors(errors)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<EntityResponse<Void>> handleGeneric(Exception ex,
                                                              HttpServletRequest request) {
        EntityResponse<Void> body = EntityResponse.<Void>builder()
                .success(false)
                .message("Internal server error")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
