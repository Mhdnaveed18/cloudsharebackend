package com.cloudshareoriginal.repository;

import com.cloudshareoriginal.model.FileShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileShareRepository extends JpaRepository<FileShare, Long> {
    boolean existsByFile_IdAndSharedTo_Id(Long fileId, Long sharedToId);
    Optional<FileShare> findByFile_IdAndSharedTo_Id(Long fileId, Long sharedToId);
    List<FileShare> findAllBySharedTo_IdOrderBySharedOnDesc(Long userId);
    List<FileShare> findAllByOwner_IdOrderBySharedOnDesc(Long ownerId);
}