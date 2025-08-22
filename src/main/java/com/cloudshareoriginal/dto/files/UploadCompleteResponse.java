package com.cloudshareoriginal.dto.files;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadCompleteResponse {
    private List<FileSummary> files;
    private Quota quota; // Use our own Quota type here

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileSummary {
        private Long id;
        private String name;
        private String contentType;
        private Long size;
        private String visibility;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Quota {
        private Long used;
        private Long max; // This field is now supported by Lombok's builder
    }
}
