package com.cloudshareoriginal.dto.files;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadResponse {
    private Long id;
    private String name;
    private String contentType;
    private Long size;
    private String visibility; // "PUBLIC" | "PRIVATE"
    private String fileUrl;
}
