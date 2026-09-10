package com.aijapanese.speaking.conversation.entity;

import com.aijapanese.speaking.speaking.entity.SpeakingSession;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_settings")
public class ConversationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private SpeakingSession session;

    @Column(name = "situation", length = 100, nullable = true)
    private String situation;

    @Column(name = "partner_role", length = 100, nullable = true)
    private String partnerRole;

    @Column(name = "partner_personality", length = 100, nullable = true)
    private String partnerPersonality;

    @Column(name = "situation_description", nullable = true, columnDefinition = "TEXT")
    private String situationDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", length = 20, nullable = false)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "subtitle_mode", length = 30, nullable = false)
    private SubtitleMode subtitleMode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ConversationSetting() {
    }

    public ConversationSetting(
            SpeakingSession session,
            String situation,
            String partnerRole,
            String partnerPersonality,
            String situationDescription,
            Difficulty difficulty,
            SubtitleMode subtitleMode,
            LocalDateTime createdAt
    ) {
        this.session = session;
        this.situation = situation;
        this.partnerRole = partnerRole;
        this.partnerPersonality = partnerPersonality;
        this.situationDescription = situationDescription;
        this.difficulty = difficulty;
        this.subtitleMode = subtitleMode;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public SpeakingSession getSession() {
        return session;
    }

    public String getSituation() {
        return situation;
    }

    public String getPartnerRole() {
        return partnerRole;
    }

    public String getPartnerPersonality() {
        return partnerPersonality;
    }

    public String getSituationDescription() {
        return situationDescription;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public SubtitleMode getSubtitleMode() {
        return subtitleMode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
