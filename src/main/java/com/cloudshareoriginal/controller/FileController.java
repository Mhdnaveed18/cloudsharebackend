package com.cloudshareoriginal.controller;

import com.cloudshareoriginal.dto.EntityResponse;
import com.cloudshareoriginal.dto.files.*;
import com.cloudshareoriginal.model.FileItem;
import com.cloudshareoriginal.model.FileShare;
import com.cloudshareoriginal.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EntityResponse<UploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest http) {

        System.out.println("coming here");
        // 1️⃣ Validate file size (use configured max size)
        if (file.getSize() > fileService.getMaxFileSizeBytes()) {
            EntityResponse<UploadResponse> body = EntityResponse.<UploadResponse>builder()
                    .success(false)
                    .message("File too large (max " + fileService.getMaxFileSizeBytes() + " bytes)")
                    .timestamp(Instant.now())
                    .path(http.getRequestURI())
                    .build();
            return ResponseEntity.badRequest().body(body);
        }

        // 2️⃣ Upload file directly and get metadata
        FileItem uploaded = fileService.uploadFile(file);

        // 3️⃣ Return uploaded file info to client
        UploadResponse data = UploadResponse.builder()
                .id(uploaded.getId())
                .name(uploaded.getOriginalName())
                .contentType(uploaded.getContentType())
                .size(uploaded.getSize())
                .visibility(uploaded.getVisibility().name())
                .fileUrl(uploaded.getS3Key())
                .build();

        EntityResponse<UploadResponse> body = EntityResponse.<UploadResponse>builder()
                .success(true)
                .message("File uploaded")
                .data(data)
                .timestamp(Instant.now())
                .path(http.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }


    @GetMapping
    public ResponseEntity<EntityResponse<List<FileItem>>> list(@RequestParam(value = "visibility", required = false) FileItem.Visibility visibility,
                                                               HttpServletRequest http) {
        List<FileItem> data = fileService.list(visibility);
        EntityResponse<List<FileItem>> body = EntityResponse.<List<FileItem>>builder()
                .success(true)
                .message("Files fetched")
                .data(data)
                .timestamp(Instant.now())
                .path(http.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<EntityResponse<FileSummaryResponse>> toggle(@PathVariable Long id,
                                                           @Valid @RequestBody ToggleVisibilityRequest req,
                                                           HttpServletRequest http) {
        FileItem updated = fileService.toggleVisibility(id, req.getVisibility());
        FileSummaryResponse data = toSummary(updated, true);

        EntityResponse<FileSummaryResponse> body = EntityResponse.<FileSummaryResponse>builder()
                .success(true)
                .message("Visibility updated")
                .data(data)
                .timestamp(Instant.now())
                .path(http.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<EntityResponse<FileSummaryResponse>> viewFile(@PathVariable Long id, HttpServletRequest http) {
        try {
            FileItem file = fileService.getFileForView(id);
            FileSummaryResponse data;
            String message;
            boolean isOwner = fileService.isCurrentUserOwner(file);
            boolean isShared = fileService.isSharedWithCurrentUser(file);

            if (file.getVisibility() == FileItem.Visibility.PUBLIC) {
                // Public file: show URL to everyone
                data = toSummary(file, true);
                message = "File is public. URL is available.";
            } else if (isOwner) {
                // Private file, owner: show URL
                data = toSummary(file, true);
                message = "File is private. Only you can view and access the URL.";
            } else if (isShared) {
                // Private file, shared to me: show URL
                data = toSummary(file, true);
                message = "File is private but shared with you. URL is available.";
            } else {
                // Private file, not owner and not shared: do not show URL
                data = toSummary(file, false);
                message = "This file is private and cannot be viewed or accessed by other users.";
            }

            EntityResponse<FileSummaryResponse> body = EntityResponse.<FileSummaryResponse>builder()
                    .success(file.getVisibility() == FileItem.Visibility.PUBLIC || isOwner || isShared)
                    .message(message)
                    .data(data)
                    .timestamp(Instant.now())
                    .path(http.getRequestURI())
                    .build();
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException ex) {
            EntityResponse<FileSummaryResponse> body = EntityResponse.<FileSummaryResponse>builder()
                    .success(false)
                    .message("File not found with id: " + id)
                    .timestamp(Instant.now())
                    .path(http.getRequestURI())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        } catch (IllegalStateException ex) {
            EntityResponse<FileSummaryResponse> body = EntityResponse.<FileSummaryResponse>builder()
                    .success(false)
                    .message("This file is private and cannot be viewed or accessed by other users.")
                    .timestamp(Instant.now())
                    .path(http.getRequestURI())
                    .build();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }
    }

    private FileSummaryResponse toSummary(FileItem file, boolean includeUrl) {
        return FileSummaryResponse.builder()
                .id(file.getId())
                .name(file.getOriginalName())
                .contentType(file.getContentType())
                .size(file.getSize())
                .visibility(file.getVisibility().name())
                .fileUrl(includeUrl ? file.getS3Key() : null)
                .favorite(file.isFavorite())
                .build();
    }

    private SharedFileResponse toShared(FileShare share, boolean includeUrl, boolean ownedFlag) {
        FileItem file = share.getFile();
        return SharedFileResponse.builder()
                .fileId(file.getId())
                .name(file.getOriginalName())
                .contentType(file.getContentType())
                .size(file.getSize())
                .visibility(file.getVisibility().name())
                .fileUrl(includeUrl ? file.getS3Key() : null)
                .owned(ownedFlag)
                .sharedBy(share.getOwner().getEmail())
                .sharedTo(share.getSharedTo().getEmail())
                .sharedOn(share.getSharedOn())
                .build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<EntityResponse<List<FileSummaryResponse>>> getFilesByUserId(@PathVariable Long userId, HttpServletRequest http) {
        List<FileItem> files = fileService.listByUserId(userId);
        List<FileSummaryResponse> data = files.stream()
                .map(f -> toSummary(f, f.getVisibility() == FileItem.Visibility.PUBLIC))
                .collect(java.util.stream.Collectors.toList()); // <-- fix toList() error

        String message;
        if (files.isEmpty()) {
            message = "No files found for user " + userId;
        } else {
            message = "Files fetched for user " + userId;
        }

        EntityResponse<List<FileSummaryResponse>> body = EntityResponse.<List<FileSummaryResponse>>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .path(http.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/quota")
    public ResponseEntity<EntityResponse<QuotaResponse>> getQuota(HttpServletRequest http) {
        QuotaResponse data = fileService.getQuota();
        EntityResponse<QuotaResponse> body = EntityResponse.<QuotaResponse>builder()
                .success(true)
                .message("Quota fetched")
                .data(data)
                .timestamp(Instant.now())
                .path(http.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}/download-url")
    public ResponseEntity<EntityResponse<DownloadUrlResponse>> getDownloadUrl(@PathVariable Long id, HttpServletRequest http) {
        try {
            FileItem file = fileService.getFileForView(id);
            boolean isOwner = fileService.isCurrentUserOwner(file);
            boolean isShared = fileService.isSharedWithCurrentUser(file);
            boolean canAccessUrl = file.getVisibility() == FileItem.Visibility.PUBLIC || isOwner || isShared;

            DownloadUrlResponse data = new DownloadUrlResponse(canAccessUrl ? file.getS3Key() : null);
            String message = canAccessUrl ? "Download URL generated" : "You are not allowed to access the download URL for this file.";

            EntityResponse<DownloadUrlResponse> body = EntityResponse.<DownloadUrlResponse>builder()
                    .success(canAccessUrl)
                    .message(message)
                    .data(data)
                    .timestamp(Instant.now())
                    .path(http.getRequestURI())
                    .build();
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException ex) {
            EntityResponse<DownloadUrlResponse> body = EntityResponse.<DownloadUrlResponse>builder()
                    .success(false)
                    .message("File not found with id: " + id)
                    .timestamp(Instant.now())
                    .path(http.getRequestURI())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        } catch (IllegalStateException ex) {
            EntityResponse<DownloadUrlResponse> body = EntityResponse.<DownloadUrlResponse>builder()
                    .success(false)
                    .message("This file is private and cannot be accessed by other users.")
                    .timestamp(Instant.now())
                    .path(http.getRequestURI())
                    .build();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }
    }

    @PatchMapping("/{id}/favorite")
    public ResponseEntity<EntityResponse<FileSummaryResponse>> setFavorite(@PathVariable Long id,
                                                                           @RequestParam("value") boolean value,
                                                                           HttpServletRequest http) {
        try {
            FileItem updated = fileService.setFavorite(id, value);
            FileSummaryResponse data = toSummary(updated, true);
            EntityResponse<FileSummaryResponse> body = EntityResponse.<FileSummaryResponse>builder()
                    .success(true)
                    .message(value ? "Marked as favorite" : "Removed from favorites")
                    .data(data)
                    .timestamp(Instant.now())
                    .path(http.getRequestURI())
                    .build();
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException ex) {
            EntityResponse<FileSummaryResponse> body = EntityResponse.<FileSummaryResponse>builder()
                    .success(false)
                    .message("File not found or not owned by you")
                    .timestamp(Instant.now())
                    .path(http.getRequestURI())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
    }

    @GetMapping("/favorites")
    public ResponseEntity<EntityResponse<List<FileSummaryResponse>>> listFavorites(HttpServletRequest http) {
        List<FileItem> favs = fileService.listFavorites();
        List<FileSummaryResponse> data = favs.stream()
                .map(f -> toSummary(f, true))
                .collect(java.util.stream.Collectors.toList());
        EntityResponse<List<FileSummaryResponse>> body = EntityResponse.<List<FileSummaryResponse>>builder()
                .success(true)
                .message("Favorite files fetched")
                .data(data)
                .timestamp(Instant.now())
                .path(http.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }

    @PostMapping(path = "/{id}/share", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityResponse<SharedFileResponse>> share(@PathVariable Long id,
                                                                    @Valid @RequestBody ShareFileRequest req,
                                                                    HttpServletRequest http) {
        try {
            FileShare share = fileService.shareFile(id, req.getEmail());
            SharedFileResponse data = toShared(share, true, true);
            EntityResponse<SharedFileResponse> body = EntityResponse.<SharedFileResponse>builder()
                    .success(true)
                    .message("File shared successfully")
                    .data(data)
                    .timestamp(Instant.now())
                    .path(http.getRequestURI())
                    .build();
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException ex) {
            EntityResponse<SharedFileResponse> body = EntityResponse.<SharedFileResponse>builder()
                    .success(false)
                    .message(ex.getMessage())
                    .timestamp(Instant.now())
                    .path(http.getRequestURI())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }
    }

    @GetMapping("/shared/with-me")
    public ResponseEntity<EntityResponse<List<SharedFileResponse>>> listSharedWithMe(HttpServletRequest http) {
        List<FileShare> shares = fileService.listSharedWithMe();
        List<SharedFileResponse> data = shares.stream()
                .map(s -> toShared(s, true, false))
                .collect(java.util.stream.Collectors.toList());
        EntityResponse<List<SharedFileResponse>> body = EntityResponse.<List<SharedFileResponse>>builder()
                .success(true)
                .message("Shared files fetched")
                .data(data)
                .timestamp(Instant.now())
                .path(http.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/shared/by-me")
    public ResponseEntity<EntityResponse<List<SharedFileResponse>>> listSharedByMe(HttpServletRequest http) {
        List<FileShare> shares = fileService.listSharedByMe();
        List<SharedFileResponse> data = shares.stream()
                .map(s -> toShared(s, true, true))
                .collect(java.util.stream.Collectors.toList());
        EntityResponse<List<SharedFileResponse>> body = EntityResponse.<List<SharedFileResponse>>builder()
                .success(true)
                .message("Shared files by me fetched")
                .data(data)
                .timestamp(Instant.now())
                .path(http.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }
}
