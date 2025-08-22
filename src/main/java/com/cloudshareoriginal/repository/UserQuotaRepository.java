package com.cloudshareoriginal.repository;

import com.cloudshareoriginal.model.UserQuota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserQuotaRepository extends JpaRepository<UserQuota, Long> {
    Optional<UserQuota> findByUserId(Long userId);
    Optional<UserQuota> findBySubscriptionId(String subscriptionId);
}
