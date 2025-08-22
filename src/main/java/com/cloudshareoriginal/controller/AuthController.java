package com.cloudshareoriginal.controller;

import com.cloudshareoriginal.dto.*;
import com.cloudshareoriginal.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<EntityResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                                 HttpServletRequest httpRequest) {
        AuthResponse data = authService.register(request);
        EntityResponse<AuthResponse> body = EntityResponse.<AuthResponse>builder()
                .success(true)
                .message("Registration successful")
                .data(data)
                .timestamp(Instant.now())
                .path(httpRequest.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/login")
    public ResponseEntity<EntityResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                              HttpServletRequest httpRequest) {
        AuthResponse data = authService.login(request);
        EntityResponse<AuthResponse> body = EntityResponse.<AuthResponse>builder()
                .success(true)
                .message("Login successful")
                .data(data)
                .timestamp(Instant.now())
                .path(httpRequest.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<EntityResponse<ForgotPasswordResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        ForgotPasswordResponse data = authService.forgotPassword(request);
        EntityResponse<ForgotPasswordResponse> body = EntityResponse.<ForgotPasswordResponse>builder()
                .success(true)
                .message("If the email exists, a reset token has been generated.")
                .data(data)
                .timestamp(Instant.now())
                .path(httpRequest.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/verify")
    public ResponseEntity<EntityResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request,
                                                            HttpServletRequest httpRequest) {
        authService.verifyEmail(request);
        EntityResponse<Void> body = EntityResponse.<Void>builder()
                .success(true)
                .message("Email verified successfully")
                .timestamp(Instant.now())
                .path(httpRequest.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/verify/send")
    public ResponseEntity<EntityResponse<Void>> sendVerificationCode(@Valid @RequestBody ForgotPasswordRequest request,
                                                                     HttpServletRequest httpRequest) {
        authService.sendVerificationCode(request.getEmail());
        EntityResponse<Void> body = EntityResponse.<Void>builder()
                .success(true)
                .message("If the email exists, a verification code has been sent.")
                .timestamp(Instant.now())
                .path(httpRequest.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<EntityResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                                              HttpServletRequest httpRequest) {
        authService.resetPassword(request);
        EntityResponse<Void> body = EntityResponse.<Void>builder()
                .success(true)
                .message("Password reset successful")
                .timestamp(Instant.now())
                .path(httpRequest.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/verify/status")
    public ResponseEntity<EntityResponse<Boolean>> isEmailVerified(@RequestParam("email") String email,
                                                                   HttpServletRequest httpRequest) {
        boolean verified = authService.isEmailVerified(email);
        EntityResponse<Boolean> body = EntityResponse.<Boolean>builder()
                .success(true)
                .message("Email verification status")
                .data(verified)
                .timestamp(Instant.now())
                .path(httpRequest.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }
}
