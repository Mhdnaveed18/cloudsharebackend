package com.cloudshareoriginal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ForgotPasswordResponse {
    private String message;
    // For development/testing purposes, return the token so it can be used to reset.
    private String resetToken;
}
