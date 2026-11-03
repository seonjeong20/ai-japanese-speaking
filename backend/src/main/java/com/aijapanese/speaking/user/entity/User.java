package com.aijapanese.speaking.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_users_provider_provider_user_id",
                columnNames = {"provider", "provider_user_id"}
        )
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 20, nullable = false)
    private OAuthProvider provider;

    @Column(name = "provider_user_id", length = 255, nullable = false)
    private String providerUserId;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private UserStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at", nullable = true)
    private LocalDateTime lastLoginAt;

    protected User() {
    }

    /**
     * 소셜 로그인으로 처음 가입한 사용자. 역할은 LEARNER, 상태는 ACTIVE로 시작한다.
     */
    public User(
            OAuthProvider provider,
            String providerUserId,
            String name,
            String email,
            LocalDateTime createdAt
    ) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.name = name;
        this.email = email;
        this.role = UserRole.LEARNER;
        this.status = UserStatus.ACTIVE;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public OAuthProvider getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void updateLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
