package com.cloudshareoriginal.service;

import com.cloudshareoriginal.dto.files.*;
import com.cloudshareoriginal.model.FileItem;
import com.cloudshareoriginal.model.FileShare;
import com.cloudshareoriginal.model.User;
import com.cloudshareoriginal.repository.FileItemRepository;
import com.cloudshareoriginal.repository.FileShareRepository;
import com.cloudshareoriginal.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Service s3Service;
    private final UserRepository userRepository;
    private final FileItemRepository fileItemRepository;
    private final FileShareRepository fileShareRepository;
    private final QuotaService quotaService;

    @Value("${app.files.max-per-upload:5}")
    private int maxFilesPerUpload;

    @Getter
    @Value("${app.files.max-size-bytes:10485760}")
    private long maxFileSizeBytes;

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<FileItem> list(FileItem.Visibility visibility) {
        User user = currentUser();
        if (visibility == null) {
            return fileItemRepository.findAllByOwnerIdOrderByCreatedAtDesc(user.getId());
        }
        return fileItemRepository.findAllByOwnerIdAndVisibilityOrderByCreatedAtDesc(user.getId(), visibility);
    }

    @Transactional(readOnly = true)
    public List<FileItem> listFavorites() {
        User user = currentUser();
        return fileItemRepository.findAllByOwnerIdAndFavoriteTrueOrderByCreatedAtDesc(user.getId());
    }

    @Transactional
    public FileItem setFavorite(Long id, boolean favorite) {
        User user = currentUser();
        FileItem fi = fileItemRepository.findByIdAndOwnerId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        fi.setFavorite(favorite);
        return fileItemRepository.save(fi);
    }

    @Transactional(readOnly = true)
    public List<FileItem> listByUserId(Long userId) {
        // Only return public files for other users, all files for self
        User current = currentUser();
        if (current.getId().equals(userId)) {
            return fileItemRepository.findAllByOwnerIdOrderByCreatedAtDesc(userId);
        } else {
            return fileItemRepository.findAllByOwnerIdAndVisibilityOrderByCreatedAtDesc(userId, FileItem.Visibility.PUBLIC);
        }
    }

    @Transactional
    public FileItem toggleVisibility(Long id, FileItem.Visibility visibility) {
        User user = currentUser();
        FileItem fi = fileItemRepository.findByIdAndOwnerId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        fi.setVisibility(visibility);
        return fileItemRepository.save(fi);
    }

    @Transactional
    public void delete(Long id) {
        User user = currentUser();
        FileItem fi = fileItemRepository.findByIdAndOwnerId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        // Actually delete from S3
        s3Service.deleteObject(extractS3Key(fi.getS3Key()));

        // Remove from database
        fileItemRepository.delete(fi);

        // Update quota
        quotaService.decrementUsed(user, 1);
    }

    // Helper to extract S3 key from URL if needed
    private String extractS3Key(String s3UrlOrKey) {
        if (s3UrlOrKey.startsWith("https://")) {
            int idx = s3UrlOrKey.lastIndexOf("/");
            return idx != -1 ? s3UrlOrKey.substring(idx + 1) : s3UrlOrKey;
        }
        return s3UrlOrKey;
    }

    @Transactional
    public FileItem uploadFile(MultipartFile file) {
        User user = currentUser();

        // Require verified email to upload
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Email not verified. Please verify your email to upload files.");
        }

        int remaining = quotaService.remaining(user);
        if (remaining < 1) {
            boolean subscribed = quotaService.isSubscribed(user);
            int limit = quotaService.getOrCreate(user).getLimitFiles();
            if (subscribed) {
                throw new IllegalStateException("You have reached your plan limit of " + limit + " files. Please delete some files or upgrade.");
            } else {
                throw new IllegalStateException("Free plan limit reached. Please purchase the Pro plan to upload up to " + subscriptionFileLimit + " files.");
            }
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("File too large: " + file.getOriginalFilename());
        }

        String s3Url = s3Service.uploadFile(user.getId(), file);

        FileItem fi = FileItem.builder()
                .owner(user)
                .originalName(file.getOriginalFilename())
                .s3Key(s3Url) // Store the S3 URL
                .contentType(file.getContentType())
                .size(file.getSize())
                .visibility(FileItem.Visibility.PRIVATE)
                .status(FileItem.Status.READY)
                .build();
        fileItemRepository.save(fi);

        quotaService.incrementUsed(user, 1);

        return fi;
    }

    @Value("${app.subscription.file-limit:100}")
    private int subscriptionFileLimit;

    @Transactional(readOnly = true)
    public FileItem getFileForView(Long id) {
        FileItem file = fileItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        if (file.getVisibility() == FileItem.Visibility.PUBLIC) {
            return file;
        }
        User user = currentUser();
        // Owner can view
        if (file.getOwner().getId().equals(user.getId())) {
            return file;
        }
        // If private but shared to current user, allow view
        boolean isShared = fileShareRepository.existsByFile_IdAndSharedTo_Id(file.getId(), user.getId());
        if (isShared) {
            return file;
        }
        throw new IllegalStateException("You do not have permission to view this file.");
    }

    // Utility to check if current user is owner of the file
    public boolean isCurrentUserOwner(FileItem file) {
        User user = currentUser();
        return file.getOwner().getId().equals(user.getId());
    }

    @Transactional
    public FileShare shareFile(Long fileId, String recipientEmail) {
        User owner = currentUser();
        FileItem file = fileItemRepository.findByIdAndOwnerId(fileId, owner.getId())
                .orElseThrow(() -> new IllegalArgumentException("File not found or not owned by you"));
        User recipient = userRepository.findByEmail(recipientEmail)
                .orElseThrow(() -> new IllegalArgumentException("Recipient is not a registered user"));
        if (recipient.getId().equals(owner.getId())) {
            throw new IllegalArgumentException("You cannot share a file with yourself");
        }
        if (fileShareRepository.existsByFile_IdAndSharedTo_Id(file.getId(), recipient.getId())) {
            return fileShareRepository.findByFile_IdAndSharedTo_Id(file.getId(), recipient.getId()).get();
        }
        FileShare share = FileShare.builder()
                .file(file)
                .owner(owner)
                .sharedTo(recipient)
                .build();
        return fileShareRepository.save(share);
    }

    @Transactional(readOnly = true)
    public List<FileShare> listSharedWithMe() {
        User me = currentUser();
        return fileShareRepository.findAllBySharedTo_IdOrderBySharedOnDesc(me.getId());
    }

    @Transactional(readOnly = true)
    public List<FileShare> listSharedByMe() {
        User me = currentUser();
        return fileShareRepository.findAllByOwner_IdOrderBySharedOnDesc(me.getId());
    }

    @Transactional(readOnly = true)
    public boolean isSharedWithCurrentUser(FileItem file) {
        User me = currentUser();
        return fileShareRepository.existsByFile_IdAndSharedTo_Id(file.getId(), me.getId());
    }

    private int getLimit(User user) {
        return quotaService.getOrCreate(user).getLimitFiles();
    }

    private int getUsedCount(User user) {
        return quotaService.getOrCreate(user).getUsedFiles();
    }

    public QuotaResponse getQuota() {
        User user = currentUser();
        var q = quotaService.getOrCreate(user);
        int remaining = Math.max(0, q.getLimitFiles() - q.getUsedFiles());
        return QuotaResponse.builder()
                .used(q.getUsedFiles())
                .limit(q.getLimitFiles())
                .remaining(remaining)
                .plan(q.getSubscriptionStatus())
                .build();
    }
}
