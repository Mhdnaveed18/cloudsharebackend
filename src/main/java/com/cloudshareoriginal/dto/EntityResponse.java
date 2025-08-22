package com.cloudshareoriginal.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class EntityResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Instant timestamp;
    private String path;
    private Map<String, Object> errors;
}
