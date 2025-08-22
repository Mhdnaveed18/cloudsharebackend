package com.cloudshareoriginal.service;

import com.cloudshareoriginal.model.User;
import com.cloudshareoriginal.model.UserQuota;
import com.cloudshareoriginal.repository.FileItemRepository;
import com.cloudshareoriginal.repository.UserQuotaRepository;
import com.cloudshareoriginal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuotaService {

    private final UserQuotaRepository userQuotaRepository;
    private final UserRepository userRepository;
    private final FileItemRepository fileItemRepository;

    @Value("${app.subscription.file-limit:100}")
    private int subscriptionFileLimit;

    @Value("${app.free.file-limit:5}")
    private int freeFileLimit;

    @Transactional
    public UserQuota getOrCreate(User user) {
        return userQuotaRepository.findByUserId(user.getId())
                .orElseGet(() -> userQuotaRepository.save(UserQuota.builder()
                        .user(user)
                        .limitFiles(freeFileLimit) // default to free tier limit for new/inactive users
                        .usedFiles(0)
                        .subscriptionStatus("inactive")
                        .build()));
    }

    @Transactional(readOnly = true)
    public int remaining(User user) {
        UserQuota q = getOrCreate(user);
        // used from table in case it's updated async; also cross-check with repository if needed
        return Math.max(0, q.getLimitFiles() - q.getUsedFiles());
    }

    @Transactional
    public void incrementUsed(User user, int delta) {
        UserQuota q = getOrCreate(user);
        q.setUsedFiles(q.getUsedFiles() + delta);
        userQuotaRepository.save(q);
    }

    @Transactional
    public void decrementUsed(User user, int delta) {
        UserQuota q = getOrCreate(user);
        q.setUsedFiles(Math.max(0, q.getUsedFiles() - delta));
        userQuotaRepository.save(q);
    }

    @Transactional
    public void activateSubscription(User user, String subscriptionId, int limit) {
        UserQuota q = getOrCreate(user);
        q.setSubscriptionId(subscriptionId);
        q.setSubscriptionStatus("active");
        q.setLimitFiles(limit);
        userQuotaRepository.save(q);
    }

    @Transactional
    public void setSubscriptionStatusById(String subscriptionId, String status) {
        userQuotaRepository.findBySubscriptionId(subscriptionId).ifPresent(q -> {
            q.setSubscriptionStatus(status);
            if ("inactive".equalsIgnoreCase(status)) {
                // Revert to free tier limit but not below current used count
                q.setLimitFiles(Math.max(freeFileLimit, q.getUsedFiles()));
            }
            userQuotaRepository.save(q);
        });
    }

    // Add this method to check if a user is subscribed (based on UserQuota)
    public boolean isSubscribed(User user) {
        UserQuota q = getOrCreate(user);
        return "active".equalsIgnoreCase(q.getSubscriptionStatus());
    }
}
