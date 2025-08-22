package com.cloudshareoriginal.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_quota", indexes = @Index(name = "idx_quota_user", columnList = "user_id", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Builder.Default
    private Integer limitFiles = 0;

    @Builder.Default
    private Integer usedFiles = 0;

    private String subscriptionId;

    @Builder.Default
    private String subscriptionStatus = "inactive";

    private Instant currentPeriodEnd;
}
