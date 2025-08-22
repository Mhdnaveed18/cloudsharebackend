package com.cloudshareoriginal.service;

import com.cloudshareoriginal.dto.UserEmailResponse;
import com.cloudshareoriginal.dto.UserProfileResponse;
import com.cloudshareoriginal.model.User;
import com.cloudshareoriginal.repository.UserRepository;
import com.cloudshareoriginal.repository.FileItemRepository;
import com.cloudshareoriginal.repository.UserQuotaRepository;
import com.cloudshareoriginal.model.FileItem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final QuotaService quotaService;
    private final FileItemRepository fileItemRepository;
    private final UserQuotaRepository userQuotaRepository;

    @Value("${app.profile.max-size-bytes:5242880}")
    private long maxProfileSizeBytes;

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email).orElseThrow();
    }

    public UserProfileResponse getCurrentUserProfile() {
        User user = currentUser();
        boolean premium = quotaService.isSubscribed(user);
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .profileImageUrl(user.getProfileImageUrl())
                .emailVerified(Boolean.TRUE.equals(user.getEmailVerified()))
                .premium(premium)
                .build();
    }

    public List<UserEmailResponse> searchUserEmails(String query, int limit) {
        if (query == null) query = "";
        String q = query.trim();
        if (q.length() < 1) {
            return List.of();
        }
        int capped = Math.max(1, Math.min(limit <= 0 ? 10 : limit, 20));
        User me = currentUser();
        // repository provides top 10; if caller asks less, we'll subList; if more, we still cap at 10
        List<User> initial = userRepository.findTop10ByEmailStartingWithIgnoreCase(q);
        // exclude self and map
        List<UserEmailResponse> mapped = initial.stream()
                .filter(u -> !u.getId().equals(me.getId()))
                .map(u -> UserEmailResponse.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .firstName(u.getFirstName())
                        .lastName(u.getLastName())
                        .profileImageUrl(u.getProfileImageUrl())
                        .build())
                .collect(Collectors.toList());
        if (mapped.size() > capped) {
            return mapped.subList(0, capped);
        }
        return mapped;
    }

    public List<UserEmailResponse> listUserEmails(int limit) {
        int capped = Math.max(1, Math.min(limit <= 0 ? 10 : limit, 50));
        User me = currentUser();
        List<User> initial = userRepository.findTop50ByIdNotOrderByEmailAsc(me.getId());
        List<UserEmailResponse> mapped = initial.stream()
                .map(u -> UserEmailResponse.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .firstName(u.getFirstName())
                        .lastName(u.getLastName())
                        .profileImageUrl(u.getProfileImageUrl())
                        .build())
                .collect(Collectors.toList());
        if (mapped.size() > capped) {
            return mapped.subList(0, capped);
        }
        return mapped;
    }

    @Transactional
    public String uploadProfilePhoto(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new IllegalArgumentException("No photo provided");
        }
        if (photo.getSize() > maxProfileSizeBytes) {
            throw new IllegalArgumentException("Profile photo is too large");
        }
        String contentType = photo.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        User user = currentUser();
        String url = s3Service.uploadProfilePhoto(user.getId(), photo);
        user.setProfileImageUrl(url);
        userRepository.save(user);
        return url;
    }

    @Transactional
    public void deleteCurrentUser() {
        User user = currentUser();

        // Delete all files (DB + S3)
        java.util.List<FileItem> files = fileItemRepository.findAllByOwnerIdOrderByCreatedAtDesc(user.getId());
        for (FileItem fi : files) {
            String key = extractS3Key(fi.getS3Key());
            try {
                s3Service.deleteObject(key);
            } catch (Exception e) {
                // Log and continue to ensure account deletion proceeds
                System.err.println("Failed to delete S3 object for file id " + fi.getId() + ": " + e.getMessage());
            }
        }
        if (!files.isEmpty()) {
            fileItemRepository.deleteAll(files);
        }

        // Delete profile photo from S3 if present
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isBlank()) {
            try {
                String profileKey = extractS3Key(user.getProfileImageUrl());
                s3Service.deleteObject(profileKey);
            } catch (Exception e) {
                System.err.println("Failed to delete profile photo: " + e.getMessage());
            }
        }

        // Delete quota row if exists
        userQuotaRepository.findByUserId(user.getId()).ifPresent(userQuotaRepository::delete);

        // Finally delete the user account
        userRepository.delete(user);

        // Optionally clear security context (not strictly required)
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    private String extractS3Key(String s3UrlOrKey) {
        if (s3UrlOrKey == null) return null;
        if (s3UrlOrKey.startsWith("https://")) {
            int idx = s3UrlOrKey.indexOf("amazonaws.com/");
            if (idx != -1) {
                return s3UrlOrKey.substring(idx + "amazonaws.com/".length());
            }
            int last = s3UrlOrKey.lastIndexOf("/");
            return last != -1 ? s3UrlOrKey.substring(last + 1) : s3UrlOrKey;
        }
        return s3UrlOrKey;
    }
}