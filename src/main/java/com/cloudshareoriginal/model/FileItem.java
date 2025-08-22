package com.cloudshareoriginal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "file_items", indexes = {
        @Index(name = "idx_file_owner", columnList = "owner_id"),
        @Index(name = "idx_file_visibility", columnList = "visibility")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileItem {

    public enum Visibility { PUBLIC, PRIVATE }
    public enum Status { UPLOADING, READY, DELETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(length = 300)
    private String originalName;

    @Column(nullable = false, length = 512)
    private String s3Key;

    @Column(length = 120)
    private String contentType;

    private Long size;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Visibility visibility = Visibility.PRIVATE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @Builder.Default
    private Status status = Status.UPLOADING;

    @Builder.Default
    private Long downloadCount = 0L;

    @Builder.Default
    @Column(nullable = false)
    private boolean favorite = false;

    @Column(length = 64, unique = true)
    private String uploadToken; // tempId used during upload-init/complete

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
