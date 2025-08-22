package com.cloudshareoriginal.dto.files;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class UploadInitRequest {
    @NotNull
    @Max(5)
    private List<FileSpec> files;

    @Data
    public static class FileSpec {
        @NotBlank
        private String fileName;
        @NotBlank
        private String contentType;
        @NotNull
        @Positive
        private long size; // Changed from Long to long

        public FileSpec(String fileName, long size, String contentType) {
            this.fileName = fileName;
            this.size = size;
            this.contentType = contentType;
        }
    }
}
