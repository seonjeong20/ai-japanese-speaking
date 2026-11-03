package com.aijapanese.speaking.preference.entity;

import com.aijapanese.speaking.conversation.entity.Difficulty;
import com.aijapanese.speaking.conversation.entity.SubtitleMode;
import com.aijapanese.speaking.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * 사용자별 학습 기본값(난이도·자막). 사용자당 한 행이다.
 * 사용자를 지우면(회원 탈퇴) DB의 ON DELETE CASCADE로 함께 지워진다.
 */
@Entity
@Table(name = "user_preferences")
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'INTERMEDIATE'")
    @Column(name = "default_difficulty", length = 20, nullable = false)
    private Difficulty defaultDifficulty;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'JAPANESE_KOREAN'")
    @Column(name = "default_subtitle_mode", length = 30, nullable = false)
    private SubtitleMode defaultSubtitleMode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    protected UserPreference() {
    }

    public UserPreference(
            User user,
            Difficulty defaultDifficulty,
            SubtitleMode defaultSubtitleMode,
            LocalDateTime createdAt
    ) {
        this.user = user;
        this.defaultDifficulty = defaultDifficulty;
        this.defaultSubtitleMode = defaultSubtitleMode;
        this.createdAt = createdAt;
    }

    public void update(Difficulty defaultDifficulty, SubtitleMode defaultSubtitleMode, LocalDateTime updatedAt) {
        this.defaultDifficulty = defaultDifficulty;
        this.defaultSubtitleMode = defaultSubtitleMode;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Difficulty getDefaultDifficulty() {
        return defaultDifficulty;
    }

    public SubtitleMode getDefaultSubtitleMode() {
        return defaultSubtitleMode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
