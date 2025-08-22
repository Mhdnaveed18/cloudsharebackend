package com.cloudshareoriginal.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String profileImageUrl;
    private Boolean emailVerified;
    private Boolean premium; // true if user has an active subscription
}