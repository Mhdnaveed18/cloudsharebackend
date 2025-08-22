package com.cloudshareoriginal.dto.billing;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for creating a subscription.
 *
 * Only a single field is required:
 * - planId: The Razorpay Plan ID (e.g., "plan_MnoPqRsTuVwXyZ").
 *   Obtain this from your Razorpay Dashboard when you create a Plan.
 */
@Data
public class SubscriptionRequest {
    /**
     * Razorpay Plan ID. Must not be blank.
     */
    @NotBlank
    private String planId;
}
