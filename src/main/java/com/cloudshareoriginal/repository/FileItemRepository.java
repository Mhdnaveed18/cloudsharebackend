package com.cloudshareoriginal.repository;

import com.cloudshareoriginal.model.FileItem;
import com.cloudshareoriginal.model.FileItem.Status;
import com.cloudshareoriginal.model.FileItem.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileItemRepository extends JpaRepository<FileItem, Long> {
    long countByOwnerIdAndStatusNot(Long ownerId, Status status);
    Optional<FileItem> findByIdAndOwnerId(Long id, Long ownerId);
    Optional<FileItem> findByUploadTokenAndOwnerId(String uploadToken, Long ownerId);
    List<FileItem> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<FileItem> findAllByOwnerIdAndVisibilityOrderByCreatedAtDesc(Long ownerId, Visibility visibility);
    List<FileItem> findAllByOwnerIdAndFavoriteTrueOrderByCreatedAtDesc(Long ownerId);
}
