package com.aijapanese.speaking.speaking.entity;

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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "speaking_messages",
        uniqueConstraints = @UniqueConstraint(name = "uk_speaking_messages_session_sequence", columnNames = {"session_id", "sequence_no"})
)
public class SpeakingMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private SpeakingSession session;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "speaker", length = 20, nullable = false)
    private Speaker speaker;

    @Column(name = "message_type", length = 30, nullable = false)
    private String messageType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected SpeakingMessage() {
    }

    public SpeakingMessage(
            SpeakingSession session,
            int sequenceNo,
            Speaker speaker,
            String messageType,
            String content,
            LocalDateTime createdAt
    ) {
        this.session = session;
        this.sequenceNo = sequenceNo;
        this.speaker = speaker;
        this.messageType = messageType;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public SpeakingSession getSession() {
        return session;
    }

    public int getSequenceNo() {
        return sequenceNo;
    }

    public Speaker getSpeaker() {
        return speaker;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
