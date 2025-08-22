package com.cloudshareoriginal.controller;

import com.cloudshareoriginal.dto.EntityResponse;
import com.cloudshareoriginal.dto.billing.SubscriptionRequest;
import com.cloudshareoriginal.dto.billing.SubscriptionResponse;
import com.cloudshareoriginal.model.User;
import com.cloudshareoriginal.repository.UserRepository;
import com.cloudshareoriginal.service.QuotaService;
import com.cloudshareoriginal.service.RazorpayService;
import com.cloudshareoriginal.dto.files.QuotaResponse;
import com.cloudshareoriginal.dto.billing.PaymentOrderResponse;
import com.cloudshareoriginal.dto.billing.PaymentVerifyRequest;
import com.cloudshareoriginal.dto.billing.PaymentLinkResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final RazorpayService razorpayService;
    private final UserRepository userRepository;
    private final QuotaService quotaService;

    @Value("${app.subscription.file-limit:100}")
    private int subscriptionFileLimit;

    @Value("${app.subscription.price-inr:500}")
    private int subscriptionPriceInr;

    @Value("${app.payment.callback-url:}")
    private String paymentCallbackUrl;

    @PostMapping("/payment/order")
    public ResponseEntity<EntityResponse<PaymentOrderResponse>> createPaymentOrder(HttpServletRequest http) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElseThrow();

        int amountInPaise = subscriptionPriceInr * 100;

        Map<String, Object> order = razorpayService.createOneTimeOrder(amountInPaise, "INR", user.getId().toString(), "rcpt_" + user.getId() + "_" + System.currentTimeMillis());
        PaymentOrderResponse data = PaymentOrderResponse.builder()
                .orderId(order.get("orderId").toString())
                .amount((Integer) order.get("amount"))
                .currency(order.get("currency").toString())
                .razorpayKey(order.get("keyId").toString())
                .build();

        EntityResponse<PaymentOrderResponse> body = EntityResponse.<PaymentOrderResponse>builder()
                .success(true)
                .message("Order created")
                .data(data)
                .timestamp(Instant.now())
                .path(http.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/payment/link")
    public ResponseEntity<EntityResponse<PaymentLinkResponse>> createPaymentLink(HttpServletRequest http) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElseThrow();

        int amountInPaise = subscriptionPriceInr * 100;
        String reference = "ref_" + user.getId() + "_" + System.currentTimeMillis();

        Map<String, Object> pl = razorpayService.createPaymentLink(
                amountInPaise,
                "INR",
                user.getId().toString(),
                email,
                reference,
                paymentCallbackUrl
        );

        PaymentLinkResponse data = PaymentLinkResponse.builder()
                .paymentLinkId(pl.get("paymentLinkId").toString())
                .shortUrl(pl.get("shortUrl").toString())
                .amount((Integer) pl.get("amount"))
                .currency(pl.get("currency").toString())
                .status(pl.get("status").toString())
                .build();

        EntityResponse<PaymentLinkResponse> body = EntityResponse.<PaymentLinkResponse>builder()
                .success(true)
                .message("Payment link created. Redirect the user to shortUrl to complete payment.")
                .data(data)
                .timestamp(Instant.now())
                .path(http.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/payment/verify")
    public ResponseEntity<EntityResponse<String>> verifyPayment(@Valid @RequestBody PaymentVerifyRequest request,
                                                                HttpServletRequest http) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElseThrow();

        boolean ok = razorpayService.verifyPaymentSignature(request.getOrderId(), request.getPaymentId(), request.getSignature());
        if (!ok) {
            EntityResponse<String> body = EntityResponse.<String>builder()
                    .success(false)
                    .message("Invalid payment signature")
                    .data("failed")
                    .timestamp(Instant.now())
                    .path(http.getRequestURI())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }
        // On success, grant one-time quota upgrade
        quotaService.activateSubscription(user, "one-time-" + request.getOrderId(), subscriptionFileLimit);
        EntityResponse<String> body = EntityResponse.<String>builder()
                .success(true)
                .message("Payment verified. Quota upgraded.")
                .data("ok")
                .timestamp(Instant.now())
                .path(http.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }


    @GetMapping("/status")
    public ResponseEntity<EntityResponse<QuotaResponse>> status(HttpServletRequest http) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        var q = quotaService.getOrCreate(user);
        int remaining = Math.max(0, q.getLimitFiles() - q.getUsedFiles());
        boolean subscribed = "active".equalsIgnoreCase(q.getSubscriptionStatus());

        QuotaResponse data = QuotaResponse.builder()
                .used(q.getUsedFiles())
                .limit(q.getLimitFiles())
                .remaining(remaining)
                .plan(q.getSubscriptionStatus())
                .build();

        String message = subscribed
                ? "Pro plan active. You can upload up to " + q.getLimitFiles() + " files."
                : "No active plan. Free plan in effect. Please purchase the Pro plan to get up to " + subscriptionFileLimit + " files.";

        EntityResponse<QuotaResponse> body = EntityResponse.<QuotaResponse>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .path(http.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }
}
