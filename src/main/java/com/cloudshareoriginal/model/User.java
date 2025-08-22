package com.cloudshareoriginal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Persist actual email column
    @Column(name = "email", nullable = false, unique = true, length = 320)
    private String email;

    // Mirror column for systems expecting 'username' to be non-null
    @Column(name = "username", nullable = false, unique = true, length = 320)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "role", length = 30)
    @Builder.Default
    private String role = "USER";

    @Column(length = 100)
    private String resetToken;

    private Instant resetTokenExpiry;

    @Column(length = 16)
    private String verificationCode;

    private Instant verificationCodeExpiry;

    @Builder.Default
    private Boolean emailVerified = false;

    @Column(length = 512)
    private String profileImageUrl;

    @PrePersist
    @PreUpdate
    private void syncUsername() {
        if ((this.username == null || this.username.isBlank()) && this.email != null) {
            this.username = this.email;
        }
        if ((this.email == null || this.email.isBlank()) && this.username != null) {
            this.email = this.username;
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null || role.isBlank()) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.enabled);
    }
}
