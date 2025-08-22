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
public class UploadInitResponse {

    private List<Item> items;
    private Quota quota;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String tempId;
        private String s3Key;
        private String uploadUrl;
        private String contentType;
        private long maxSize;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Quota {
        private int used;
        private int limit;
    }
}
