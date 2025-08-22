package com.cloudshareoriginal.dto.files;

import com.cloudshareoriginal.model.FileItem;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UploadCompleteRequest {

    @NotEmpty
    private List<Item> items;

    @NotNull
    private FileItem.Visibility defaultVisibility = FileItem.Visibility.PRIVATE;

    @Data
    public static class Item {
        @NotNull
        private String tempId;
        @NotNull
        private String fileName;
        @NotNull
        private String contentType;
        @NotNull
        private Long size;
    }
}
