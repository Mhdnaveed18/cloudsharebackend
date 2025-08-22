package com.cloudshareoriginal.dto.files;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShareFileRequest {
    @NotBlank
    @Email
    private String email; // recipient email (must be registered)
}
