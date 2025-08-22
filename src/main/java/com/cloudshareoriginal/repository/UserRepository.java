package com.cloudshareoriginal.repository;

import com.cloudshareoriginal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    Optional<User> findByResetToken(String resetToken);

    List<User> findTop10ByEmailStartingWithIgnoreCase(String emailPrefix);

    List<User> findTop50ByIdNotOrderByEmailAsc(Long excludeUserId);
}
