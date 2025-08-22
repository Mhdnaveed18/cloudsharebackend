package com.cloudshareoriginal.dto.files;

import com.cloudshareoriginal.model.FileItem;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ToggleVisibilityRequest {
    @NotNull
    private FileItem.Visibility visibility;
}
