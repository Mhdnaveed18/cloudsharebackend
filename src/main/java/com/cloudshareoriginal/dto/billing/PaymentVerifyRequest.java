package com.cloudshareoriginal.dto.billing;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentVerifyRequest {
    @NotBlank
    private String orderId;
    @NotBlank
    private String paymentId;
    @NotBlank
    private String signature; // razorpay_signature
}
