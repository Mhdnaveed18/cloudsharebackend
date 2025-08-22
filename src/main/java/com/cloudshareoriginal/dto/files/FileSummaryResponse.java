package com.cloudshareoriginal.dto.files;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileSummaryResponse {
    private Long id;
    private String name;
    private String contentType;
    private Long size;
    private String visibility; // "PUBLIC" | "PRIVATE"
    private String fileUrl;    // may be null for private files
    private Boolean favorite;  // whether this file is marked favorite by the owner
}
