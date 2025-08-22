package com.cloudshareoriginal.dto.files;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaResponse {
    private int used;
    private int limit;
    private int remaining;
    private String plan; // optional
}
