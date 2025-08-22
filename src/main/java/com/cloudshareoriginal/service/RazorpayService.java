package com.cloudshareoriginal.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RazorpayService {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;


    public Map<String, Object> createSubscription(String planId, String userId, String userEmail) throws RazorpayException {
        // Kept for backwards compatibility if still called elsewhere, but subscriptions are deprecated in favor of one-time payments.
        String id = keyId == null ? "" : keyId.trim();
        String secret = keySecret == null ? "" : keySecret.trim();
        if (id.isEmpty() || secret.isEmpty()) {
            throw new IllegalStateException("Razorpay API keys are not configured. Please set properties 'razorpay.key-id' and 'razorpay.key-secret' or environment variables RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }
        RazorpayClient client = new RazorpayClient(id, secret);

        Map<String, Object> customer = new HashMap<>();
        customer.put("name", userEmail);
        customer.put("email", userEmail);

        Map<String, Object> notes = new HashMap<>();
        notes.put("userId", userId);

        Map<String, Object> params = new HashMap<>();
        params.put("plan_id", planId);
        params.put("customer_notify", 1);
        params.put("total_count", 12); // e.g. monthly plan
        params.put("notes", notes);
        params.put("customer", customer);

        var subscription = client.subscriptions.create(new JSONObject(params));
        Map<String, Object> out = new HashMap<>();
        out.put("subscriptionId", subscription.get("id"));
        out.put("status", subscription.get("status"));
        out.put("keyId", keyId);
        return out;
    }

    public Map<String, Object> createOneTimeOrder(int amountPaise, String currency, String userId, String receipt) throws RazorpayException {
        String id = keyId == null ? "" : keyId.trim();
        String secret = keySecret == null ? "" : keySecret.trim();
        if (id.isEmpty() || secret.isEmpty()) {
            throw new IllegalStateException("Razorpay API keys are not configured. Please set properties 'razorpay.key-id' and 'razorpay.key-secret' or environment variables RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }
        RazorpayClient client = new RazorpayClient(id, secret);

        Map<String, Object> notes = new HashMap<>();
        notes.put("userId", userId);

        Map<String, Object> params = new HashMap<>();
        params.put("amount", amountPaise);
        params.put("currency", currency);
        params.put("receipt", receipt);
        params.put("notes", notes);
        params.put("payment_capture", 1);

        var order = client.orders.create(new JSONObject(params));
        Map<String, Object> out = new HashMap<>();
        out.put("orderId", order.get("id"));
        out.put("amount", order.get("amount"));
        out.put("currency", order.get("currency"));
        out.put("keyId", id);
        return out;
    }

    public Map<String, Object> createPaymentLink(int amountPaise, String currency, String userId, String userEmail, String reference, String callbackUrl) throws RazorpayException {
        String id = keyId == null ? "" : keyId.trim();
        String secret = keySecret == null ? "" : keySecret.trim();
        if (id.isEmpty() || secret.isEmpty()) {
            throw new IllegalStateException("Razorpay API keys are not configured. Please set properties 'razorpay.key-id' and 'razorpay.key-secret' or environment variables RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }
        RazorpayClient client = new RazorpayClient(id, secret);

        Map<String, Object> customer = new HashMap<>();
        customer.put("name", userEmail);
        customer.put("email", userEmail);

        Map<String, Object> notify = new HashMap<>();
        notify.put("email", true);
        notify.put("sms", false);

        Map<String, Object> notes = new HashMap<>();
        notes.put("userId", userId);

        Map<String, Object> params = new HashMap<>();
        params.put("amount", amountPaise);
        params.put("currency", currency);
        params.put("accept_partial", false);
        params.put("reference_id", reference);
        params.put("description", "CloudShare Pro plan purchase");
        params.put("customer", customer);
        params.put("notify", notify);
        params.put("notes", notes);
        if (callbackUrl != null && !callbackUrl.isBlank()) {
            Map<String, Object> reminderEnable = new HashMap<>();
            params.put("callback_url", callbackUrl);
            params.put("callback_method", "get");
        }

        var pl = client.paymentLink.create(new JSONObject(params));
        Map<String, Object> out = new HashMap<>();
        out.put("paymentLinkId", pl.get("id"));
        out.put("shortUrl", pl.get("short_url"));
        out.put("status", pl.get("status"));
        out.put("amount", pl.get("amount"));
        out.put("currency", pl.get("currency"));
        return out;
    }

    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            String toSign = orderId + '|' + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            String secret = keySecret == null ? "" : keySecret.trim();
            if (secret.isEmpty()) return false;
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(toSign.getBytes(StandardCharsets.UTF_8));
            String expected = Hex.encodeHexString(digest);
            return Objects.equals(expected, signature);
        } catch (Exception e) {
            return false;
        }
    }

}
