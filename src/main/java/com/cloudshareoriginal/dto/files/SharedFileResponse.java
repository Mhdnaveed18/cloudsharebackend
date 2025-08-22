package com.cloudshareoriginal.dto.files;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedFileResponse {
    private Long fileId;
    private String name;
    private String contentType;
    private Long size;
    private String visibility;
    private String fileUrl; // include for owner or recipient

    private boolean owned; // true if current user is owner
    private String sharedBy; // email of owner
    private String sharedTo; // email of recipient
    private Instant sharedOn;
}
