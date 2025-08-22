package com.cloudshareoriginal.controller;

import com.cloudshareoriginal.dto.EntityResponse;
import com.cloudshareoriginal.dto.UserEmailResponse;
import com.cloudshareoriginal.dto.UserProfileResponse;
import com.cloudshareoriginal.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<EntityResponse<UserProfileResponse>> getCurrentUser(HttpServletRequest request) {
        UserProfileResponse profile = userService.getCurrentUserProfile();
        EntityResponse<UserProfileResponse> body = EntityResponse.<UserProfileResponse>builder()
                .success(true)
                .message("User details fetched successfully")
                .data(profile)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/search")
    public ResponseEntity<EntityResponse<List<UserEmailResponse>>> searchUsers(
            @RequestParam(name = "query") String query,
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit,
            HttpServletRequest request) {
        List<UserEmailResponse> results = userService.searchUserEmails(query, limit);
        EntityResponse<List<UserEmailResponse>> body = EntityResponse.<List<UserEmailResponse>>builder()
                .success(true)
                .message("User emails fetched")
                .data(results)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/emails")
    public ResponseEntity<EntityResponse<List<UserEmailResponse>>> listUsers(
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit,
            HttpServletRequest request) {
        List<UserEmailResponse> results = userService.listUserEmails(limit);
        EntityResponse<List<UserEmailResponse>> body = EntityResponse.<List<UserEmailResponse>>builder()
                .success(true)
                .message("User emails fetched")
                .data(results)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }

    @PostMapping(value = "/profile/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EntityResponse<String>> uploadProfilePhoto(@RequestPart("photo") MultipartFile photo,
                                                                     HttpServletRequest request) {
        String url = userService.uploadProfilePhoto(photo);
        EntityResponse<String> body = EntityResponse.<String>builder()
                .success(true)
                .message("Profile photo uploaded successfully")
                .data(url)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<EntityResponse<String>> deleteCurrentUser(HttpServletRequest request) {
        userService.deleteCurrentUser();
        EntityResponse<String> body = EntityResponse.<String>builder()
                .success(true)
                .message("Account deleted successfully")
                .data("deleted")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.ok(body);
    }
}
