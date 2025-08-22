package com.cloudshareoriginal.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "file_shares",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_file_recipient", columnNames = {"file_id", "shared_to_id"})
        },
        indexes = {
                @Index(name = "idx_shared_to", columnList = "shared_to_id"),
                @Index(name = "idx_shared_owner", columnList = "owner_id"),
                @Index(name = "idx_shared_file", columnList = "file_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private FileItem file;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_to_id")
    private User sharedTo;

    @Column(nullable = false)
    private Instant sharedOn;

    @PrePersist
    public void prePersist() {
        if (sharedOn == null) sharedOn = Instant.now();
    }
}
