package com.aijapanese.speaking.speaking.entity;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "speaking_sessions")
public class SpeakingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", length = 20, nullable = false)
    private SessionType sessionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SessionStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at", nullable = true)
    private LocalDateTime endedAt;

    @Column(name = "duration_seconds", nullable = true)
    private Integer durationSeconds;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected SpeakingSession() {
    }

    public SpeakingSession(
            User user,
            SessionType sessionType,
            SessionStatus status,
            LocalDateTime startedAt,
            LocalDateTime createdAt
    ) {
        this.user = user;
        this.sessionType = sessionType;
        this.status = status;
        this.startedAt = startedAt;
        this.createdAt = createdAt;
    }

    public void complete(LocalDateTime endedAt) {
        this.status = SessionStatus.COMPLETED;
        this.endedAt = endedAt;
        this.durationSeconds = (int) Duration.between(this.startedAt, endedAt).getSeconds();
    }

    public void abort(LocalDateTime endedAt) {
        this.status = SessionStatus.ABORTED;
        this.endedAt = endedAt;
        this.durationSeconds = (int) Duration.between(this.startedAt, endedAt).getSeconds();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
