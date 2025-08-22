package com.cloudshareoriginal.service;

import com.cloudshareoriginal.dto.*;
import com.cloudshareoriginal.security.JwtService;
import com.cloudshareoriginal.model.User;
import com.cloudshareoriginal.repository.UserRepository;
import com.cloudshareoriginal.utils.EmailUtils;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final MailService mailService;

    @Value("${app.reset-password.base-url:/reset-password}")
    private String resetPasswordBaseUrl;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       MailService mailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.mailService = mailService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = EmailUtils.normalize(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .enabled(true)
                .role("USER")
                .build();
        User saved = userRepository.save(user);

        // Create a 6-character verification code (hex) and email it
        String verificationCode = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toLowerCase();
        saved.setVerificationCode(verificationCode);
        saved.setVerificationCodeExpiry(Instant.now().plus(24, ChronoUnit.HOURS));
        userRepository.save(saved);
        mailService.sendEmailVerification(saved.getEmail(), verificationCode);

        String token = jwtService.generateToken(saved.getUsername(), saved.getAuthorities()
                .stream().map(GrantedAuthority::getAuthority).toList());
        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request) {
        String email = EmailUtils.normalize(request.getEmail());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        String token = jwtService.generateToken(user.getUsername(),
                user.getAuthorities().stream().map(a -> a.getAuthority()).toList());
        return new AuthResponse(token);
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String email = EmailUtils.normalize(request.getEmail());
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Do not reveal whether email exists
            return new ForgotPasswordResponse("If that account exists, a reset link has been generated.", null);
        }
        User user = userOpt.get();
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(Instant.now().plus(15, ChronoUnit.MINUTES));
        userRepository.save(user);

        // Build a user-facing reset link and send it via email
        String resetLink;
        if (resetPasswordBaseUrl != null && resetPasswordBaseUrl.contains("{token}")) {
            resetLink = resetPasswordBaseUrl.replace("{token}", token);
        } else {
            String sep = (resetPasswordBaseUrl != null && resetPasswordBaseUrl.contains("?")) ? "&" : "?";
            String base = (resetPasswordBaseUrl != null && !resetPasswordBaseUrl.isBlank())
                    ? resetPasswordBaseUrl
                    : "/reset-password";
            resetLink = base + sep + "token=" + token;
        }
        mailService.sendPasswordResetEmail(user.getEmail(), resetLink);

        return new ForgotPasswordResponse("Password reset token generated.", token);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reset token"));
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token expired");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        String email = EmailUtils.normalize(request.getEmail());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email or code"));

        if (user.getVerificationCode() == null
                || user.getVerificationCodeExpiry() == null
                || user.getVerificationCodeExpiry().isBefore(Instant.now())
                || !user.getVerificationCode().equalsIgnoreCase(request.getVerificationCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email or code");
        }

        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);
        userRepository.save(user);
    }

    @Transactional
    public void sendVerificationCode(String email) {
        String normalized = EmailUtils.normalize(email);
        Optional<User> userOpt = userRepository.findByEmail(normalized);
        if (userOpt.isEmpty()) {
            // Do not reveal whether email exists
            return;
        }
        User user = userOpt.get();
        String verificationCode = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toLowerCase();
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiry(Instant.now().plus(24, ChronoUnit.HOURS));
        userRepository.save(user);
        mailService.sendEmailVerification(user.getEmail(), verificationCode);
    }

    public boolean isEmailVerified(String email) {
        String normalized = EmailUtils.normalize(email);
        if (normalized == null || normalized.isBlank()) {
            return false;
        }
        return userRepository.findByEmail(normalized)
                .map(u -> Boolean.TRUE.equals(u.getEmailVerified()))
                .orElse(false);
    }
}